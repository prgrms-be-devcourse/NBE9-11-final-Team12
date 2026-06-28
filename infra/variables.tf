variable "prefix" {
  description = "Prefix for all resources"
  default     = "team12"
}

variable "region" {
  description = "region"
  default     = "ap-northeast-2"
}

variable "s3_speech_image_bucket_name" {
  description = "S3 bucket name for speech opinion images"
  type        = string
  default     = "team12-speech-images"
}

variable "frontend_origin" {
  description = "Frontend origin allowed to upload images through presigned URLs"
  type        = string
  default     = "https://www.issuetok.site"
}

variable "team12_ai_worker_role_arn" {
  description = "External AI worker IAM role ARN allowed to read/delete messages from speech event SQS queue"
  type        = string
  default     = "arn:aws:iam::878311411155:role/team12-ai-worker-role"
}