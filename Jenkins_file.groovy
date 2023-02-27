// Set up AWS credentials
withCredentials([[
  $class: 'AmazonWebServicesCredentialsBinding',
  accessKeyVariable: 'AWS_ACCESS_KEY_ID',
  credentialsId: 'aws-creds',
  secretKeyVariable: 'AWS_SECRET_ACCESS_KEY'
]]) {
  // Get the EKS cluster name and region from Jenkins parameters
  def clusterName = params.EKS_CLUSTER_NAME
  def region = params.AWS_REGION
  
  // Install and configure the AWS CLI
  sh "pip install awscli --upgrade --user"
  sh "aws configure set region ${region}"
  
  // Retrieve the EKS cluster endpoint and CA certificate
  def eksEndpoint = sh(returnStdout: true, script: "aws eks describe-cluster --name ${clusterName} --query 'cluster.endpoint' --output text").trim()
  def eksCACert = sh(returnStdout: true, script: "aws eks describe-cluster --name ${clusterName} --query 'cluster.certificateAuthority.data' --output text").trim()
  
  // Install and configure kubectl
  sh "curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl"
  sh "chmod +x kubectl"
  sh "sudo mv kubectl /usr/local/bin/"
  
  // Configure kubectl to use the EKS cluster
  sh "mkdir -p ~/.kube"
  sh "echo '${eksCACert}' | base64 --decode > ~/.kube/ca.crt"
  sh "kubectl config set-cluster ${clusterName} --server=${eksEndpoint} --certificate-authority=$HOME/.kube/ca.crt"
  sh "kubectl config set-context ${clusterName} --cluster=${clusterName} --user=aws"
  sh "kubectl config use-context ${clusterName}"
}
