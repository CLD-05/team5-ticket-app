#!/bin/bash
awslocal sqs create-queue --queue-name booking-queue --region ap-northeast-2
awslocal s3 mb s3://team5-dev-poster-bucket --region ap-northeast-2
echo "LocalStack SQS booking-queue and S3 team5-dev-poster-bucket created successfully."
