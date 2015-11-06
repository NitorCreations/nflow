nFlow performance testing
=========================

 - `ansible/` directory contains [Ansible](http://www.ansible.com/) scripts for setting up AWS performance test environment
 - `com.nitorcreations.nflow.performance.client and .server` packages contain performance test client and server implementations
 - `com.nitorcreations.nflow.performance.testdata` package contains tools for generating data for performance testing
 - `com.nitorcreations.nflow.performance.workflow` package contains workflow definitions that are utilized by other packages

**Generate data for performance testing**

Test data is created by `NflowPerfTestServer`, when it is started with `generateTestData` argument. The test data generation process is controlled through the following properties:
 - `testdata.target.count` - amount of generated workflow instances (default 100000)
 - `testdata.batch.size` - size of batch in which generated workflow instances are inserted into database (default 10000)

`NflowPerfTestServer` will not process workflow instances when started with `generateTestData` argument (nflow.autostart=false).

Example: generate 1000000 workflow instances
```
java -Dnflow.db.user=nflow -Dnflow.db.password=nflownflow 
  -Dnflow.db.postgresql.url=jdbc:postgresql://<your_rds_database>:5432/nflow?tcpKeepAlive=true 
  -Dnflow.executor.group=nflow-perf -Dnflow.non_spring_workflows_filename=workflows.txt 
  -Dspring.profiles.active=nflow.db.postgresql -Dtestdata.target.count=1000000
  -jar nflow/nflow-perf-test/target/nflow-perf-test-*-SNAPSHOT.jar generateTestData
```

**Setup AWS environment using Ansible.**

1. Install Ansible to your workstation
   * configure AWS access and secret keys ([see instructions](https://aws.amazon.com/blogs/apn/getting-started-with-ansible-and-dynamic-amazon-ec2-inventory-management/))
   * copy your AWS private key file to nflow-perf-test/ansible directory
 
2. Configure Ansible with your AWS account details (nflow-perf-test/ansible/vars/perf-test-environment.yml)
   * ec2_region (e.g. eu-west-1)
   * instances_keypair: name of your AWS private key file
   * image_id: AMI for the nFlow performance test servers
   * perftest_subnet: your VPC identifier
   * perftest_group_ids: security groups for nFlow performance test servers

3. Create 2 nFlow servers, 1 client server, 1 Graphite server and 1 RDS database
   * execute "ansible-playbook provision_ec2.yml"
   * currently does not configure load balancer in front of nFlow servers, so configure this manually to port 7500

4. Deploy nFlow to AWS
   * execute "ansible-playbook -i /etc/ansible/ec2.py --private-key=nbank.pem deploy_nflow.yml"
   * installs required services (e.g. git and Maven) and builds nFlow from Github sources
   
5. Optional: Install Graphite server by following [instructions](https://github.com/dmichel1/ansible-graphite)  
   
**Execute performance tests**

1. Connect to your nFlow servers and start servers using the following command (change parameters to match your environment):
```
java -Dhost=<your_nflow_server> -Dnflow.db.user=nflow -Dnflow.db.password=nflownflow 
  -Dnflow.db.postgresql.url=jdbc:postgresql://<your_rds_database>:5432/nflow?tcpKeepAlive=true 
  -Dnflow.executor.group=nflow-perf -Dnflow.non_spring_workflows_filename=workflows.txt 
  -Dspring.profiles.active=nflow.db.postgresql -Dgraphite.host=<your_graphite_server> -Dgraphite.port=2003 
  -jar nflow/nflow-perf-test/target/nflow-perf-tests-*-SNAPSHOT.jar
```

2. Connect to your nFlow client server and start performance client threads using the following command (change parameters to match your environment):
```
java -Dnflow.url=http://<your_nflow_server_load_balancer>:7500 -cp nflow-perf-test/target/nflow-perf-tests-*-SNAPSHOT.jar com.nitorcreations.nflow.performance.client.LoadGenerator
```