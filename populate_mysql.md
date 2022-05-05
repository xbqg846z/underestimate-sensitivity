

connect with python
```python
import mysql.connector
connection = mysql.connector.connect(user='root',
                              host='127.0.0.1',
                              database='sensitivity_attack')

# Create a cursor to perform database operations
cursor = connection.cursor()
```


open a mysql terminal
```shell
mysql -h 127.0.0.1 -P 3306 -u root -D sensitivity_attack --local-infile
```


load the transformed unsized 64 bit dataset
```shell
CREATE TABLE unsized_64_trans_u (col1 double);
LOAD DATA LOCAL INFILE '/Users/michael/openDP/prelim/sensitivity_attacks/unsized_64_trans_u.csv' INTO TABLE unsized_64_trans_u
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n';
```

load the transformed unsized 64 bit dataset
```shell
CREATE TABLE sized_64_4_trans_u (col1 double);

LOAD DATA LOCAL INFILE '/Users/michael/openDP/prelim/sensitivity_attacks/sized_4_trans_u.csv' INTO TABLE sized_64_4_trans_u
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n';

CREATE TABLE sized_64_4_trans_v (col1 double);

LOAD DATA LOCAL INFILE '/Users/michael/openDP/prelim/sensitivity_attacks/sized_4_trans_v.csv' INTO TABLE sized_64_4_trans_v
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n';
```


load the sized 64 bit dataset where position 45 is flipped
```shell
CREATE TABLE sized_6_45_u (uid int, val int);

LOAD DATA LOCAL INFILE '/Users/michael/openDP/prelim/sensitivity_attacks/sized_6_45_u.csv' INTO TABLE sized_6_45_u
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n';

CREATE TABLE sized_6_45_v (uid int, val int);

LOAD DATA LOCAL INFILE '/Users/michael/openDP/prelim/sensitivity_attacks/sized_6_45_v.csv' INTO TABLE sized_6_45_v
FIELDS TERMINATED BY ','
LINES TERMINATED BY '\n';
```