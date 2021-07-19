CURRENT_DATE=`date '+%Y/%m/%d'`
PRACTICE=$(basename $PWD)
mvn clean package -Dmaven.test.skip=true;
java -jar ./target/investec-avs-batch-0.0.1-SNAPSHOT.jar "item= jordan" "run.date(date)= $CURRENT_DATE" "practice= $PRACTICE";
read;

