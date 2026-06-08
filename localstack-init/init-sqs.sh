#!/bin/bash
awslocal sqs create-queue --queue-name booking-queue --region ap-northeast-2
echo "SQS booking-queue created successfully."
