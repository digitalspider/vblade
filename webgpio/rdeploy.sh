mvn clean package
if [[ $? == 0 ]] ; then
  echo "Deploying"
  cp target/webgpio.war /opt/jetty/jetty/webapps/root.war
  scp target/webgpio.war rpi1:/opt/jetty/webapps/root.war
  #scp target/webgpio.war odroid@odr1:/opt/jetty/jetty/webapps/root.war
  echo "Done"
fi
