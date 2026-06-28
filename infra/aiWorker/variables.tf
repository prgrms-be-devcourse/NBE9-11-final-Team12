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
  default     = 20
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