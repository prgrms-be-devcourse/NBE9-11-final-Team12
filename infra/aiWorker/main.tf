terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.region
}

resource "aws_vpc" "ai_vpc" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.prefix}-ai-vpc"
  }
}

resource "aws_subnet" "ai_public_subnet" {
  vpc_id                  = aws_vpc.ai_vpc.id
  cidr_block              = var.public_subnet_cidr
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-ai-public-subnet"
  }
}

resource "aws_internet_gateway" "ai_igw" {
  vpc_id = aws_vpc.ai_vpc.id

  tags = {
    Name = "${var.prefix}-ai-igw"
  }
}

resource "aws_route_table" "ai_public_rt" {
  vpc_id = aws_vpc.ai_vpc.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.ai_igw.id
  }

  tags = {
    Name = "${var.prefix}-ai-public-rt"
  }
}

resource "aws_route_table_association" "ai_public_assoc" {
  subnet_id      = aws_subnet.ai_public_subnet.id
  route_table_id = aws_route_table.ai_public_rt.id
}

resource "aws_security_group" "ai_worker_sg" {
  name        = "${var.prefix}-ai-worker-sg"
  description = "AI Worker security group"
  vpc_id      = aws_vpc.ai_vpc.id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.prefix}-ai-worker-sg"
  }
}

resource "aws_iam_role" "ai_worker_role" {
  name = "${var.prefix}-ai-worker-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ai_worker_ssm" {
  role       = aws_iam_role.ai_worker_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_role_policy" "ai_worker_sqs_consume" {
  name = "${var.prefix}-ai-worker-sqs-consume"
  role = aws_iam_role.ai_worker_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:ChangeMessageVisibility",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl"
        ]
        Resource = var.academy_ai_report_queue_arn
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ai_worker_profile" {
  name = "${var.prefix}-ai-worker-profile"
  role = aws_iam_role.ai_worker_role.name
}

locals {
  ai_worker_user_data = <<-EOF
#!/bin/bash
set -euo pipefail

mkdir -p /opt/ai-models
mkdir -p /opt/ai-worker
mkdir -p /etc/ai-worker

cat > /etc/ai-worker/ai-worker.env.template <<ENV_TEMPLATE_EOF
AI_REPORT_QUEUE_URL=${var.academy_ai_report_queue_url}
AI_REPORT_QUEUE_REGION=${var.region}
AI_REPORT_BACKEND_BASE_URL=${var.backend_base_url}
AI_REPORT_BACKEND_TIMEOUT_SECONDS=10
AI_REPORT_WORKER_BATCH_SIZE=1
AI_REPORT_WORKER_WAIT_TIME_SECONDS=10
AI_REPORT_WORKER_VISIBILITY_TIMEOUT_SECONDS=300
AI_REPORT_MODEL_PATH=/models/Qwen2.5-7B-Instruct-Q4_K_M.gguf
AI_REPORT_TEMPERATURE=0.3
AI_REPORT_CONTEXT_SIZE=32768
AI_REPORT_MAX_TOKENS=2048
AI_REPORT_GPU_LAYERS=-1
ENV_TEMPLATE_EOF
chmod 600 /etc/ai-worker/ai-worker.env.template

if ! command -v docker >/dev/null 2>&1; then
  if command -v yum >/dev/null 2>&1; then
    yum install -y docker
  elif command -v apt-get >/dev/null 2>&1; then
    apt-get update
    apt-get install -y docker.io
  fi
fi

systemctl enable docker
systemctl start docker

cat <<'RUNNER_EOF' > /opt/ai-worker/run-worker.sh
#!/bin/bash
set -euo pipefail

REGION="${var.region}"
IMAGE="${var.ai_worker_image}"
CONTAINER_NAME="${var.ai_worker_container_name}"
MODEL_PATH="${var.ai_worker_host_model_path}"

REQUIRED_ENV_VARS=(
  AI_REPORT_QUEUE_URL
  AI_REPORT_QUEUE_REGION
  AI_REPORT_BACKEND_BASE_URL
  AI_REPORT_WORKER_TOKEN
  AI_REPORT_BACKEND_TIMEOUT_SECONDS
  AI_REPORT_WORKER_BATCH_SIZE
  AI_REPORT_WORKER_WAIT_TIME_SECONDS
  AI_REPORT_WORKER_VISIBILITY_TIMEOUT_SECONDS
  AI_REPORT_MODEL_PATH
  AI_REPORT_TEMPERATURE
  AI_REPORT_CONTEXT_SIZE
  AI_REPORT_MAX_TOKENS
  AI_REPORT_GPU_LAYERS
)

for env_var in "$${REQUIRED_ENV_VARS[@]}"; do
  if [ -z "$${!env_var:-}" ]; then
    echo "AI Worker environment variable is missing: $env_var"
    echo "Run the GitHub Actions deploy workflow once after the instance is online."
    exit 1
  fi
done

if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is not ready."
  exit 1
fi

if [ ! -f "$MODEL_PATH" ]; then
  echo "AI model file is missing: $MODEL_PATH"
  exit 1
fi

docker pull "$IMAGE"
docker rm -f "$CONTAINER_NAME" >/dev/null 2>&1 || true
docker run -d \
  --name "$CONTAINER_NAME" \
  --restart unless-stopped \
  --gpus all \
  -e AI_REPORT_QUEUE_URL \
  -e AI_REPORT_QUEUE_REGION \
  -e AI_REPORT_BACKEND_BASE_URL \
  -e AI_REPORT_WORKER_TOKEN \
  -e AI_REPORT_BACKEND_TIMEOUT_SECONDS \
  -e AI_REPORT_WORKER_BATCH_SIZE \
  -e AI_REPORT_WORKER_WAIT_TIME_SECONDS \
  -e AI_REPORT_WORKER_VISIBILITY_TIMEOUT_SECONDS \
  -e AI_REPORT_MODEL_PATH \
  -e AI_REPORT_TEMPERATURE \
  -e AI_REPORT_CONTEXT_SIZE \
  -e AI_REPORT_MAX_TOKENS \
  -e AI_REPORT_GPU_LAYERS \
  -v /opt/ai-models:/models \
  "$IMAGE"
RUNNER_EOF

chmod 700 /opt/ai-worker/run-worker.sh

cat <<'SERVICE_EOF' > /etc/systemd/system/ai-worker.service
[Unit]
Description=AI Report SQS Worker
Wants=network-online.target
After=network-online.target docker.service
Requires=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
EnvironmentFile=/etc/ai-worker/ai-worker.env
ExecStart=/opt/ai-worker/run-worker.sh
ExecStop=/bin/sh -c 'docker rm -f ${var.ai_worker_container_name} >/dev/null 2>&1 || true'

[Install]
WantedBy=multi-user.target
SERVICE_EOF

systemctl daemon-reload
systemctl enable ai-worker.service
systemctl start ai-worker.service || true
EOF
}

resource "aws_instance" "ai_worker" {
  ami                         = var.ai_worker_ami_id
  instance_type               = "g4dn.xlarge"
  subnet_id                   = aws_subnet.ai_public_subnet.id
  vpc_security_group_ids      = [aws_security_group.ai_worker_sg.id]
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.ai_worker_profile.name

  root_block_device {
    volume_type = "gp3"
    volume_size = var.root_volume_size
  }

  user_data = local.ai_worker_user_data

  tags = {
    Name = "${var.prefix}-ai-worker-1"
  }
}

output "ai_worker_instance_id" {
  value = aws_instance.ai_worker.id
}

output "ai_worker_role_arn" {
  value = aws_iam_role.ai_worker_role.arn
}

output "ai_worker_public_ip" {
  value = aws_instance.ai_worker.public_ip
}

output "ai_worker_security_group_id" {
  value = aws_security_group.ai_worker_sg.id
}
