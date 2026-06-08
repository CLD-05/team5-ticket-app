#!/bin/bash
awslocal sqs create-queue --queue-name booking-queue
echo "SQS booking-queue created successfully."
