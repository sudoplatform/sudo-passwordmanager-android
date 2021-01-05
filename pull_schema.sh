#! /bin/bash

# Make sure to set the $AWS_PROFILE env variable

echo "Pulling schema using downloaded config"

APPSYNC_ID=`jq -r '.value' ./telephony-system-test-config/telephony_ssm_parameter_graphql_id.json`
aws appsync get-introspection-schema \
  --api-id $APPSYNC_ID \
  --format 'JSON' \
  sudotelephony/src/main/graphql/com.sudoplatform.sudotelephony/schema.json

echo "Converting json schema to graphql..."
npx aws-appsync-codegen print-schema sudotelephony/src/main/graphql/com.sudoplatform.sudotelephony/schema.json \
   --output sudotelephony/src/main/graphql/com.sudoplatform.sudotelephony/schema.json.graphql
