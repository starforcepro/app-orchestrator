# app-orchestrator

Orchestrator of small applications for AWS Lambda.

![Scheme](scheme.png)

## Features
- Manage lambdas with a simple REST API. 
- Proxy requests to lambdas. Proxy deconstructs request and sends it to the lambda as a key-value map. So each lambda must have an interface that accepts and is able to parse such a map.

## Roadmap
- Add distributed storage
- Add s3 integration
- Add support of PackageType: Image
- Now proxy and manager are the same service, need to split them for better scalability
- Add better error handling
- Add more tests
- Need more strict contract for lambda applications for better proxying
- Domain names for each application
- Increased availability: multiple replicas of the manager and proxy
- Mini-apps need observability: structured logs, metrics and tracing, health/readiness probes, dashboards and alerts

Notes:
- There is need to provide AWS credentials to the orchestrator.
- Openapi spec is provided at `/openapi.json`

## Run with Docker

Build container:
```bash
docker build -t orchestrator -f .\Dockerfile .
```

Run container:
```bash
docker run --rm -p 8080:8080 \
  -e AWS_ACCESS_KEY_ID= \
  -e AWS_SECRET_ACCESS_KEY= \
  -e AWS_REGION=eu-north-1 \
  -e AWS_LAMBDA_ROLE_ARN=arn:aws:iam::<account-id>:role/<role-name> \
  orchestrator
```