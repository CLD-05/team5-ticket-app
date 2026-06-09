#!/bin/bash
ENDPOINT="http://localhost:4566"
REGION="ap-northeast-2"
aws --endpoint-url=$ENDPOINT --region $REGION sqs create-queue --queue-name booking-dlq
DLQ_URL=$(aws --endpoint-url=$ENDPOINT --region $REGION sqs get-queue-url --queue-name booking-dlq --query 'QueueUrl' --output text)
DLQ_ARN=$(aws --endpoint-url=$ENDPOINT --region $REGION sqs get-queue-attributes --queue-url "$DLQ_URL" --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)
aws --endpoint-url=$ENDPOINT --region $REGION sqs create-queue --queue-name booking-queue \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}"
aws --endpoint-url=$ENDPOINT --region $REGION sqs list-queues
