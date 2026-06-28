terraform {
  // aws 라이브러리 불러옴
  required_providers {
    aws = {
      source = "hashicorp/aws"
    }
  }
}

# AWS 설정 시작
provider "aws" {
  region = var.region
}
# AWS 설정 끝

# VPC 설정 시작
resource "aws_vpc" "vpc_1" {
  cidr_block = "10.0.0.0/16"

  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.prefix}-vpc-1"
  }
}

resource "aws_subnet" "subnet_1" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-1"
  }
}

resource "aws_subnet" "subnet_2" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-2"
  }
}

resource "aws_subnet" "subnet_3" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.3.0/24"
  availability_zone       = "${var.region}c"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-3"
  }
}

resource "aws_subnet" "subnet_4" {
  vpc_id                  = aws_vpc.vpc_1.id
  cidr_block              = "10.0.4.0/24"
  availability_zone       = "${var.region}d"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.prefix}-subnet-4"
  }
}

resource "aws_internet_gateway" "igw_1" {
  vpc_id = aws_vpc.vpc_1.id

  tags = {
    Name = "${var.prefix}-igw-1"
  }
}

resource "aws_route_table" "rt_1" {
  vpc_id = aws_vpc.vpc_1.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw_1.id
  }

  tags = {
    Name = "${var.prefix}-rt-1"
  }
}

resource "aws_route_table_association" "association_1" {
  subnet_id      = aws_subnet.subnet_1.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_2" {
  subnet_id      = aws_subnet.subnet_2.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_3" {
  subnet_id      = aws_subnet.subnet_3.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_route_table_association" "association_4" {
  subnet_id      = aws_subnet.subnet_4.id
  route_table_id = aws_route_table.rt_1.id
}

resource "aws_security_group" "app_sg" {
  name   = "${var.prefix}-app-sg"
  vpc_id = aws_vpc.vpc_1.id

  # HTTP
  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # HTTPS
  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  # NPM 관리자 페이지 - 전체 공개 X
  ingress {
    from_port   = 81
    to_port     = 81
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.prefix}-app-sg"
  }
}


resource "aws_security_group" "db_sg" {
  name   = "${var.prefix}-db-sg"
  vpc_id = aws_vpc.vpc_1.id

  # MySQL은 App EC2에서만 접근 허용
  ingress {
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.app_sg.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.prefix}-db-sg"
  }
}

# EC2 설정 시작

# # EC2 역할 생성
# resource "aws_iam_role" "ec2_role_1" {
#   name = "${var.prefix}-ec2-role-1"
#
#   # 이 역할에 대한 신뢰 정책 설정. EC2 서비스가 이 역할을 가정할 수 있도록 설정
#   assume_role_policy = <<EOF
#   {
#     "Version": "2012-10-17",
#     "Statement": [
#       {
#         "Sid": "",
#         "Action": "sts:AssumeRole",
#         "Principal": {
#             "Service": "ec2.amazonaws.com"
#         },
#         "Effect": "Allow"
#       }
#     ]
#   }
#   EOF
# }
#
# # EC2 역할에 AmazonS3FullAccess 정책을 부착
# resource "aws_iam_role_policy_attachment" "s3_full_access" {
#   role       = aws_iam_role.ec2_role_1.name
#   policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
# }
#
# # EC2 역할에 AmazonEC2RoleforSSM 정책을 부착
# resource "aws_iam_role_policy_attachment" "ec2_ssm" {
#   role       = aws_iam_role.ec2_role_1.name
#   policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonEC2RoleforSSM"
# }


# # IAM 인스턴스 프로파일 생성
# resource "aws_iam_instance_profile" "instance_profile_1" {
#   name = "${var.prefix}-instance-profile-1"
#   role = aws_iam_role.ec2_role_1.name
# }

data "aws_iam_role" "ec2_role_1" {
  name = "team12-ec2-role-1"
}

data "aws_iam_instance_profile" "instance_profile_1" {
  name = "team12-instance-profile-1"
}

# EC2 역할에 AmazonEC2RoleforSSM 정책을 부착
resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = data.aws_iam_role.ec2_role_1.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# EC2 역할에 AmazonS3FullAccess 정책을 부착
resource "aws_s3_bucket" "speech_images" {
  bucket = var.s3_speech_image_bucket_name

  tags = {
    Name = "${var.prefix}-speech-images"
  }
}

resource "aws_s3_bucket_ownership_controls" "speech_images" {
  bucket = aws_s3_bucket.speech_images.id

  rule {
    object_ownership = "BucketOwnerEnforced"
  }
}

resource "aws_s3_bucket_public_access_block" "speech_images" {
  bucket = aws_s3_bucket.speech_images.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "speech_images" {
  bucket = aws_s3_bucket.speech_images.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_cors_configuration" "speech_images" {
  bucket = aws_s3_bucket.speech_images.id

  cors_rule {
    allowed_methods = ["PUT", "HEAD", "GET"]
    allowed_origins = [var.frontend_origin]
    allowed_headers = ["*"]
    expose_headers  = ["ETag"]
    max_age_seconds = 3000
  }
}

resource "aws_iam_policy" "speech_image_s3_policy" {
  name = "${var.prefix}-speech-image-s3-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:HeadObject",
          "s3:DeleteObject"
        ]
        Resource = "${aws_s3_bucket.speech_images.arn}/speeches/*"
      },
      {
        Effect = "Allow"
        Action = [
          "s3:ListBucket"
        ]
        Resource = aws_s3_bucket.speech_images.arn
        Condition = {
          StringLike = {
            "s3:prefix" = ["speeches/*"]
          }
        }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "speech_image_s3_policy" {
  role       = data.aws_iam_role.ec2_role_1.name
  policy_arn = aws_iam_policy.speech_image_s3_policy.arn
}

output "speech_image_bucket_name" {
  value = aws_s3_bucket.speech_images.bucket
}
# SQS 설정 시작

resource "aws_sqs_queue" "speech_event_dlq" {
  name                      = "${var.prefix}-speech-event-dlq"
  message_retention_seconds = 1209600 # 14일

  tags = {
    Name = "${var.prefix}-speech-event-dlq"
  }
}

resource "aws_sqs_queue" "speech_event_queue" {
  name                       = "${var.prefix}-speech-event-queue"
  visibility_timeout_seconds = 30
  message_retention_seconds  = 345600 # 4일

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.speech_event_dlq.arn
    maxReceiveCount     = 3
  })

  tags = {
    Name = "${var.prefix}-speech-event-queue"
  }
}

resource "aws_iam_policy" "speech_event_sqs_policy" {
  name = "${var.prefix}-speech-event-sqs-policy"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueUrl",
          "sqs:ChangeMessageVisibility"
        ]
        Resource = [
          aws_sqs_queue.speech_event_queue.arn,
          aws_sqs_queue.speech_event_dlq.arn
        ]
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "speech_event_sqs_policy" {
  role       = data.aws_iam_role.ec2_role_1.name
  policy_arn = aws_iam_policy.speech_event_sqs_policy.arn
}

resource "aws_sqs_queue_policy" "allow_team12_ai_worker" {
  queue_url = aws_sqs_queue.speech_event_queue.url

  policy = jsonencode({
    Version = "2012-10-17"

    Statement = [
      {
        Sid    = "AllowTeam12AIWorkerReadDelete"
        Effect = "Allow"

        Principal = {
          AWS = "arn:aws:iam::878311411155:root"
        }

        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage"
        ]

        Resource = aws_sqs_queue.speech_event_queue.arn

        Condition = {
          ArnEquals = {
            "aws:PrincipalArn" = var.team12_ai_worker_role_arn
          }
        }
      }
    ]
  })
}

output "speech_event_queue_url" {
  value = aws_sqs_queue.speech_event_queue.url
}

output "speech_event_queue_arn" {
  value = aws_sqs_queue.speech_event_queue.arn
}

output "speech_event_dlq_url" {
  value = aws_sqs_queue.speech_event_dlq.url
}

# SQS 설정 끝

locals {
  ec2_user_data_base = <<-END_OF_FILE
#!/bin/bash

cat <<EOF >> /etc/environment
PASSWORD_1=${var.password_1}
NPM_PASSWORD=${var.npm_password}
APP_1_DOMAIN=${var.app_1_domain}
NPM_EMAIL=${var.npm_email}
SQS_SPEECH_EVENT_QUEUE_URL=${aws_sqs_queue.speech_event_queue.url}
SQS_SPEECH_EVENT_DLQ_URL=${aws_sqs_queue.speech_event_dlq.url}
EOF

# 가상 메모리 4GB 설정
sudo dd if=/dev/zero of=/swapfile bs=128M count=32
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
sudo sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# 도커 설치 및 실행/활성화
yum install docker -y
systemctl enable docker
systemctl start docker

# 도커 네트워크 생성
docker network create common

# nginx 설치
docker run -d \
  --name npm_1 \
  --restart unless-stopped \
  --network common \
  -p 80:80 \
  -p 443:443 \
  -p 81:81 \
  -e TZ=Asia/Seoul \
  -v /dockerProjects/npm_1/volumes/data:/data \
  -v /dockerProjects/npm_1/volumes/etc/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest


# redis 설치
docker run -d \
  --name redis_1 \
  --restart unless-stopped \
  --network common \
  --memory=512m \
  --memory-swap=512m \
  -e TZ=Asia/Seoul \
  redis:latest redis-server \
  --requirepass ${var.password_1} \
  --maxmemory 256mb \
  --maxmemory-policy allkeys-lru

END_OF_FILE
}


data "aws_ssm_parameter" "amazon_linux_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# EC2 인스턴스 생성
resource "aws_instance" "ec2_1" {
  # 사용할 AMI ID
  ami = data.aws_ssm_parameter.amazon_linux_ami.value
  # EC2 인스턴스 유형
  instance_type = "t3.medium"
  # 사용할 서브넷 ID
  subnet_id = aws_subnet.subnet_2.id
  # 적용할 보안 그룹 ID
  vpc_security_group_ids = [aws_security_group.app_sg.id]
  # 퍼블릭 IP 연결 설정
  associate_public_ip_address = true

  # 인스턴스에 IAM 역할 연결
  iam_instance_profile = data.aws_iam_instance_profile.instance_profile_1.name

  # 인스턴스에 태그 설정
  tags = {
    Name = "${var.prefix}-web"
  }

  # 루트 볼륨 설정
  root_block_device {
    volume_type = "gp3"
    volume_size = 30 # 볼륨 크기를 12GB로 설정
  }

  user_data = <<-EOF
${local.ec2_user_data_base}
EOF
}


resource "aws_instance" "mysql_1" {
  ami           = data.aws_ssm_parameter.amazon_linux_ami.value
  instance_type = "t3.small"

  subnet_id                   = aws_subnet.subnet_3.id
  vpc_security_group_ids      = [aws_security_group.db_sg.id]
  associate_public_ip_address = true

  iam_instance_profile = data.aws_iam_instance_profile.instance_profile_1.name

  tags = {
    Name = "${var.prefix}-mysql"
  }

  root_block_device {
    volume_type = "gp3"
    volume_size = 30
  }

  user_data = <<-EOF
#!/bin/bash

# swap 설정
dd if=/dev/zero of=/swapfile bs=128M count=32
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
sh -c 'echo "/swapfile swap swap defaults 0 0" >> /etc/fstab'

# docker 설치
yum install docker -y
systemctl enable docker
systemctl start docker

docker network create db-network || true

# MySQL 실행
docker run -d \
  --name mysql_1 \
  --restart unless-stopped \
  --network db-network \
  -p 3306:3306 \
  --memory=1200m \
  --memory-swap=1500m \
  -v /dockerProjects/mysql_1/volumes/var/lib/mysql:/var/lib/mysql \
  -v /dockerProjects/mysql_1/volumes/etc/mysql/conf.d:/etc/mysql/conf.d \
  -e MYSQL_ROOT_PASSWORD=${var.password_1} \
  -e TZ=Asia/Seoul \
  mysql:latest

echo "MySQL이 기동될 때까지 대기 중..."
until docker exec mysql_1 mysql -uroot -p${var.password_1} -e "SELECT 1" &> /dev/null; do
  echo "MySQL이 아직 준비되지 않음. 5초 후 재시도..."
  sleep 5
done

docker exec mysql_1 mysql -uroot -p${var.password_1} -e "
CREATE DATABASE IF NOT EXISTS sisibibi;

CREATE USER IF NOT EXISTS 'sisibibi'@'%' IDENTIFIED BY '${var.password_1}';

GRANT ALL PRIVILEGES ON sisibibi.* TO 'sisibibi'@'%';

FLUSH PRIVILEGES;
"
EOF
}

data "aws_eip" "eip_ec2_1" {
  filter {
    name   = "tag:Name"
    values = ["${var.prefix}-eip"]
  }
}

resource "aws_eip_association" "ec2_1" {
  instance_id   = aws_instance.ec2_1.id
  allocation_id = data.aws_eip.eip_ec2_1.id
}

# resource "aws_ec2_instance_state" "ec2_1" {
#   instance_id = aws_instance.ec2_1.id
#   state       = "stopped"
# }

output "mysql_private_ip" {
  value = aws_instance.mysql_1.private_ip
}