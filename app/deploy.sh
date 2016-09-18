mvn clean package
if [[ $? == 0 ]] ; then
  echo "Deploying"
  cp target/wifi.war /opt/jetty/jetty/webapps/root.war
  #scp target/wifi.war rpi1:/opt/jetty/webapps/root.war
  echo "Done"
  tail -f jetty.log
fi
