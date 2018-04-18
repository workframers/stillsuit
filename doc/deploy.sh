#!/usr/bin/env bash

PROJECT=stillsuit
VERSION=current

# Boneheaded script to push some stuff to S3 for static hosting
# Relies on our CircleCI AWS perms

BUILD=../target

BUCKET=docs.workframe.com
S3_ROOT=s3://${BUCKET}/${PROJECT}/${VERSION}

LOCAL_IMAGE=image
if [[ -d ${LOCAL_IMAGE} ]]; then
    S3_IMG=${S3_ROOT}/image
    echo "Copying images to ${S3_IMG}..."
    aws s3 sync --delete ${LOCAL_IMAGE} ${S3_IMG}
fi

LOCAL_SLIDES=${BUILD}/slides
if [[ -d ${LOCAL_SLIDES} ]]; then
    S3_SLIDES=${S3_ROOT}/slides
    echo "Copying slides to ${S3_SLIDES}..."
    aws s3 sync --delete ${LOCAL_SLIDES} ${S3_SLIDES}
fi

LOCAL_CODOX=${BUILD}/doc
if [[ -d ${LOCAL_CODOX} ]]; then
    S3_CODOX=${S3_ROOT}/doc
    echo "Copying codox documentation to ${S3_CODOX}..."
    aws s3 sync --delete ${LOCAL_CODOX} ${S3_CODOX}
fi

LOCAL_MANUAL=${BUILD}/manual
if [[ -d ${LOCAL_MANUAL} ]]; then
    S3_MANUAL=${S3_ROOT}/manual
    echo "Copying codox documentation to ${S3_MANUAL}..."
    aws s3 sync --delete ${LOCAL_MANUAL} ${S3_MANUAL}
fi
