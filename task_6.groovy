job("Job1-Devops_task_6")
{
	description("First Job: To download GitHub code")
        
	scm {
		github('krushnakant241/devops_proj_6', 'master')
		}
	
	triggers { 
		scm('* * * * *')
			}
	
	steps {
		shell('''if ls /home/ | grep code
				then
					echo "Directory already present"
				else
					sudo mkdir /home/code
				fi

				sudo rm -rf /home/code/*
				sudo cp -rvf * /home/code/''')
		}
}

job('Job2-Devops_task_6')
{
	description("Second Job: To deploy respective code on the kubernetes using correspondence image")

	triggers {
		upstream('Job1-Devops_task_6', 'SUCCESS')
			}
	
	steps {
		shell('''
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
''')
		}
}


job("Job3-Devops_task_6")
{
	description("Third Job: code testing")

	triggers {
		upstream('Job3-Devops_task_6','SUCCESS')
			}
	steps {
		shell('''
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
''')
		}
		
		publishers {
        downstreamParameterized {
            trigger('Job4-Devops_task_6') {
                condition('UNSTABLE_OR_FAILURER')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}

job("Job4-Devops_task_6")
{
	description("Fourth Job: Sending Mail")

	triggers {
		upstream('Job3-Devops_task_6','FAILED')
			}
	steps {
		shell('''
echo "There is a some error in code or no suitable code found, please refer the logs of job3"
exit 1
''')
		}
		
	publishers {
		extendedEmail {
			recipientList('krushnakant.ace@gmail.com')
			defaultSubject('Build failed')
			defaultContent('testing has been failed so there is error in code. Please check the code')
			contentType('text/html')
			triggers {
				beforeBuild()
				stillUnstable {
				subject('Subject')
				content('Body')
				sendTo {
					developers()
					requester()
					culprits()
					}
				}
			}
		}
	}
}

buildPipelineView("task6_build_pipeline") {
	filterBuildQueue(true)
    filterExecutors(false)
    title("Devops_task_6")
    displayedBuilds(4)
    selectedJob("Job1-Devops_task_6")
    alwaysAllowManualTrigger(true)
    showPipelineParameters(true)
    refreshFrequency(5)
}
