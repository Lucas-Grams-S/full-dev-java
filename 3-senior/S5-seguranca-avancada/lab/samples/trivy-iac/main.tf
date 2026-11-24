resource "aws_s3_bucket" "statements" {
  bucket = "payflow-statements-demo"
}

resource "aws_security_group" "db" {
  name = "payflow-db"
  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
