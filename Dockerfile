FROM centos

RUN yum install -y wget
RUN wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat/jenkins.repo
RUN rpm --import https://pkg.jenkins.io/redhat/jenkins.io.key
RUN yum install java -y && yum install jenkins -y && yum install git -y && yum install sudo -y
RUN echo -e "jenkins ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
CMD java -jar /usr/lib/jenkins/jenkins.war
EXPOSE 8080

RUN yum install -y curl
RUN curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.18.0/bin/linux/amd64/kubectl
RUN chmod +x ./kubectl
RUN mv ./kubectl /usr/bin/kubectl
RUN kubectl version --client
RUN mkdir /root/.kube
