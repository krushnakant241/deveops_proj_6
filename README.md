# DevOps_Task_6 (Kubernetes + Dockerfile + Git + Jenkinsfile + Automatic-Testing)

## Project Tasks:
Perform below task on top of Kubernetes where we use Kubernetes resources like Pods, ReplicaSet, Deployment, PVC and Service.

1. Create container image that’s has Jenkins installed  using dockerfile  Or You can use the Jenkins Server on RHEL 8/7
2. When we launch this image, it should automatically starts Jenkins service in the container.
3. Create a job chain of job1, job2, job3 and  job4 using build pipeline plugin in Jenkins 
4. Job2(seed job) : Pull  the Github repo automatically when some developers push repo to Github.
5. Further on jobs should be pipeline using written code using Groovy language bye the developer.
6. Job1 : 
    a). By looking at the code or program file, Jenkins should automatically start the respective language interpreter installed image container to deploy code on top of Kubernetes ( eg. If code is of  PHP, then Jenkins should start the container that has PHP already installed )
    b).  Expose your pod so that testing team could perform the testing on the pod
    c). Make the data to remain persistent ( If server collects some data like logs, other user information )
6. Job3 : Test your app if it  is working or not.
7. Job4 : if app is not working , then send email to developer with error messages and redeploy the application after code is being edited by the developer

## Let's see step by step how to achieve this:

#### Step - 1 -Create Dockerfile and Build Image, please find the below command, refer these snaps - (jenkins image creation).
created Dockerfile as per uploded file and builded image using below command and also run it.
```
docker build -t myjenkins:v1 . (here"." means we are running this command from present directory of Dockerfile)
```

#### Step - 2 -Run that Image using below command, refer these snaps - (Jenkins docker run, kubeconfig file and certificates of Minikube server API, minikube start and IP).
```
docker run -it -P -v /root/.kube:/root/.kube --name myjenkins1 myjenkins:v1
```
(Note: we have attached kubeconfig folder with the jenkins container folder to provide access of k8s server API which is running in my minikube machine, -P is for exposing 8080 port)

#### Step - 3 - Login in url of jenkins using below command to find exposed port, please find the below command, refer these snaps - (initial password, Exposed port).
```
docker ps
```
-By using base os ip to open jenkins(http://192.168.99.101:32771/)

-Use default password shown in this file (/root/.jenkins/secrets/initialAdminPassword) 

-Change the admin password then create below jobs

#### Step - 4 - Job-1 -Pull the code from GitHub when developers pushed to Github using poll SCM, please find the below code, refer these snaps - (Github plugin, Job-1-snap-1, Job-1-snap-2).
-First of all, install GitHub and build pipeline plugins from manage jenkins.

-pull the code from GitHub and run below command to copy those files from jenkins workspace to that folder
```
if ls /home/ | grep code
then
	echo "Directory already present"
else
	sudo mkdir /home/code
fi

sudo rm -rf /home/code/*
sudo cp -rvf * /home/code/
```

#### Step - 5 - Job-2 -this job run if job1 build successfully -it will check code, run respective deployment(PHPOS or HTMLOS), below code will first check PVC and service availability then run respective PVC and service too. please find the below code, refer these snaps - (Job-2-snap-1, Job-2-snap-2, PHP code running, HTML code running).

```
#PVC checking
if sudo kubectl get pvc | grep phpos-pvc-claim
then
	echo "PHPOS pvc already claimed"
else
	sudo kubectl create -f /home/code/phpos-pvc-claim.yml
fi
if sudo kubectl get pvc | grep htmlos-pvc-claim
then
	echo "HTMLOS pvc already claimed"
else
	sudo kubectl create -f /home/code/htmlos-pvc-claim.yml
fi


#Service checking
if sudo kubectl get svc | grep phpos
then
	echo "PHPOS-Service is already running"
else
	sudo kubectl create -f /home/code/phpos-service.yml
fi
if sudo kubectl get svc | grep htmlos
then
	echo "HTMLOS-Service is already running"
else
	sudo kubectl create -f /home/code/htmlos-service.yml
fi
```
```
#PHP code checking
if sudo ls /home/code/ | grep .php
then
	if sudo kubectl get deployment | grep phpos
	then
		echo "PHPOS Deployment is already running"
		sudo kubectl apply -f /home/code/phpos.yml
		phppodname=$(sudo kubectl get pods | grep phpos | awk '{print $1}')
		sudo kubectl cp /home/code/*.php $phppodname:/var/www/html/
    else
		echo "No Deployment is running"
        sudo kubectl create -f /home/code/phpos.yml
		phppodname=$(sudo kubectl get pods | grep phpos | awk '{print $1}')
        sleep 10
        sudo kubectl cp /home/code/*.php $phppodname:/var/www/html/
    fi
else
	echo "No PHP code is available"
fi


#HTML code checking
if sudo ls /home/code/ | grep .html
then
	if sudo kubectl get deployment | grep htmlos
	then
		echo "Deployment is already running"
		sudo kubectl apply -f /home/code/htmlos.yml
		htmlpodname=$(sudo kubectl get pods | grep htmlos | awk '{print $1}')
		sudo kubectl cp /home/code/*.html $htmlpodname:/var/www/html/
    else
		echo "No Deployment is running"
        sudo kubectl create -f /home/code/htmlos.yml
        htmlpodname=$(sudo kubectl get pods | grep htmlos | awk '{print $1}')
        sleep 10
		sudo kubectl cp /home/code/*.html $htmlpodname:/var/www/html/
    fi
else
	echo "No HTML code is available"
fi
```
#### Step - 6 - Job-3 -this job run if job2 build successfully -it will test the code, it is working or not, Here you can see IP=192.168.99.100-which is my kubernetes IP. refer these snaps - (Job-3).

```
if sudo ls /home/code/ | grep index.php
then
	export status=$(curl -o /dev/null -s -w "%{http_code}" http://192.168.99.100:30001/index.php)
	if [ $status -eq 200 ]
	then
		exit 0
	else
		echo "Error in PHP code"
		exit 1
	fi
else
	if sudo ls /home/code/ | grep index.html
	then
		export status1=$(curl -o /dev/null -s -w "%{http_code}" http://192.168.99.100:30002/index.html)
		if [ $status1 -eq 200 ]
		then
			exit 0
		else
			echo "Error in HTML code"
			exit 1
		fi
	fi
echo "No suitable code found"
exit 1
fi
```

#### Step - 7 - Job-4 -this job run if job3 build unsuccessful,unstable or failed -it will send notification to developer, refer these snaps - (job-4, Code error-failed notification).
```
echo "There is a some error in code or no suitable code found, please refer the logs of job3"
exit 1
```

#### Please refer this snap for build pipeline view - (Build pipeline).
