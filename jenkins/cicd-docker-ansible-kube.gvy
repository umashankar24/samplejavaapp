pipeline {
agent any
stages {
    stage('compile') {
	    steps { 
		    echo 'compiling..'
		    git url: 'https://github.com/umashankar24/samplejavaapp'
		    sh script: '/opt/maven/bin/mvn compile'
	    }
    }
    stage('codereview-pmd') {
	    steps { 
		    echo 'codereview..'
		    sh script: '/opt/maven/bin/mvn -P metrics pmd:pmd'
            }
	    post {
		    success {
			    recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
		    }
	    }		
    }
    stage('unit-test') {
	    steps {
		    echo 'unittest..'
		    sh script: '/opt/maven/bin/mvn test'
	    }
	    post {
		    success {
			    junit 'target/surefire-reports/*.xml'
		    }
	    }			
    }
    stage('package/build-war') {
	    steps {
		    echo 'package......'
		    sh script: '/opt/maven/bin/mvn package'	
	    }		
    }
    stage('build & push docker image') {
	    steps {
		    sh 'cd $WORKSPACE'
		    sh 'docker build --file Dockerfile --tag umashankar24/samplejavaapp:$BUILD_NUMBER .'
		//    withCredentials([string(credentialsId: 'DOCKER_HUB_PWD', variable: 'DOCKER_HUB_PWD')]) {
		//	    sh "docker login -u umashankar24 -p ${DOCKER_HUB_PWD}"
		    withCredentials([usernamePassword(credentialsId: 'DOCKER_HUB_PWD', passwordVariable: 'pass', usernameVariable: 'user')]) {
			    sh 'echo $user'
			    sh "docker login -u ${user} -p ${pass}"    
		    }
		    sh 'docker push umashankar24/samplejavaapp:$BUILD_NUMBER'
	    }
    }
    stage('Deploy-QA') {
	    steps {
		    sh 'ansible-playbook --inventory /tmp/myinv deploy/deploy-kube.yml --extra-vars "env=qa build=$BUILD_NUMBER"'
	    }
    }
}
}
