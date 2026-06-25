variable "prefix" {
  description = "Prefix for all resources"
  default     = "team12"
}

variable "region" {
  description = "region"
  default     = "ap-northeast-2"
}

variable "nickname" {
  description = "nickname"
  default     = "jhs512"
}

variable "s3_speech_image_bucket_name" {
  description = "S3 bucket name for speech opinion images"
  type        = string
  default     = "team12-speech-images"
}

variable "frontend_origin" {
  description = "Frontend origin allowed to upload images through presigned URLs"
  type        = string
  default     = "https://your-frontend-domain.example.com"
}