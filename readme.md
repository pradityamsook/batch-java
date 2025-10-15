# Kube command for batch job

## Delete job
>`kubectl delete job read-csv-job-runner -n default`

## Deploy job
>`kubectl apply -f read-csv-job-runner -n default`

## Log after deploy
>`kubectl logs -f $(kubectl get pods --selector=job-name=read-csv-job-runner -n default --output=jsonpath='{.items[0].metadata.name}') -n default`