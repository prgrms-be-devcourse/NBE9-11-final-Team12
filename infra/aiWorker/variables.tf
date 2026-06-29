variable "prefix" {
  description = "Prefix for personal AI resources"
  type        = string
  default     = "team12"
}

variable "region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "vpc_cidr" {
  description = "CIDR block for AI Worker VPC"
  type        = string
  default     = "10.20.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR block for AI Worker public subnet"
  type        = string
  default     = "10.20.1.0/24"
}

variable "ai_worker_ami_id" {
  description = "AMI ID for GPU AI Worker. Use an AMI with NVIDIA driver and Docker GPU runtime prepared."
  type        = string
}

variable "root_volume_size" {
  description = "Root EBS volume size in GiB"
  type        = number
  default     = 100
}

variable "academy_ai_report_queue_url" {
  description = "AI report SQS queue URL in academy AWS account"
  type        = string
}

variable "academy_ai_report_queue_arn" {
  description = "AI report SQS queue ARN in academy AWS account"
  type        = string
}

variable "backend_base_url" {
  description = "Spring backend base URL reachable from the AI Worker"
  type        = string
}

variable "ai_worker_image" {
  description = "AI Worker Docker image URI"
  type        = string
}

variable "ai_worker_container_name" {
  description = "AI Worker Docker container name"
  type        = string
  default     = "ai-report-worker"
}

variable "ai_worker_host_model_path" {
  description = "Model file path on the EC2 host"
  type        = string
  default     = "/opt/ai-models/Qwen2.5-7B-Instruct-Q4_K_M.gguf"
}
