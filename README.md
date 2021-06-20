flink-connector-clickhouse
flink版本1.12对应scala版本2.11
支持批量写入定时刷新
暂支持sink，后续加上source

CREATE TABLE ck_sink (
    name VARCHAR,
    grade BIGINT,
    rate FLOAT,
    more VARCHAR
) WITH (
    'connector' = 'clickhouse',
    'url' = 'clickhouse://192.168.8.94:8123',
    'username' = '',
    'password' = '',
    'database-name' = 'glab',        /* ClickHouse 数据库名，默认为 default */
    'table-name' = 'ck_test',      /* ClickHouse 数据表名 */
    'sink.batch-size' = '1000',         /* batch 大小 */
    'sink.flush-interval' = '1000',     /* flush 时间间隔 */
    'sink.max-retries' = '1',           /* 最大重试次数 */
    'sink.partition-strategy' = 'balanced', /* hash | shuffle | balanced */
    'sink.write-local' = 'true',        /*如果为ture则默认写入本地表,否则写入集群表*/
    'sink.ignore-delete' = 'true'       /* 忽略 DELETE 并视 UPDATE 为 INSERT */
)

create table if not exists ck_kafka(
   tname VARCHAR,
   tgrade BIGINT,
   trate FLOAT,
   tmore VARCHAR
")WITH(
    'connector' = 'kafka',
    'topic' = 'ck_test',
    'scan.startup.mode' = 'latest-offset',
    'properties.group.id' = 'ck_test1',
    'properties.bootstrap.servers' = 'ga-kafka1:9092,ga-kafka2:9092',
    'format' = 'csv',
    'csv.ignore-parse-errors' = 'true',
    'csv.field-delimiter' = '|',
    'csv.null-literal' = ''
)

insert into ck_sink select * from ck_kafka