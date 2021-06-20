package com.glab.flink.connector.clickhouse.table;

import com.glab.flink.connector.clickhouse.table.internal.options.ClickHouseOptions;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.factories.*;
import org.apache.flink.table.utils.TableSchemaUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ClickHouseDynamicTableFactory implements DynamicTableSinkFactory {
    public static final String IDENTIFIER = "clickhouse";

    public static final ConfigOption<String> URL = ConfigOptions.key("url")
            .stringType()
            .noDefaultValue()
            .withDeprecatedKeys("the ClickHouse url in format `clickhouse://<host>:<port>`.");
    public static final ConfigOption<String> USERNAME = ConfigOptions.key("username")
            .stringType()
            .noDefaultValue()
            .withDescription("the ClickHouse username.");

    public static final ConfigOption<String> PASSWORD = ConfigOptions.key("password")
            .stringType()
            .noDefaultValue()
            .withDescription("the ClickHouse password.");

    public static final ConfigOption<String> DATABASE_NAME = ConfigOptions.key("database-name")
            .stringType()
            .defaultValue("default")
            .withDescription("the ClickHouse database name. Default to `default`.");

    public static final ConfigOption<String> TABLE_NAME = ConfigOptions.key("table-name")
            .stringType()
            .noDefaultValue()
            .withDescription("the ClickHouse table name.");

    public static final ConfigOption<Integer> SINK_BATCH_SIZE = ConfigOptions.key("sink.batch-size")
            .intType()
            .defaultValue(Integer.valueOf(1000))
            .withDescription("the flush max size, over this number of records, will flush data. The default value is 1000.");

    public static final ConfigOption<Duration> SINK_FLUSH_INTERVAL = ConfigOptions.key("sink.flush-interval")
            .durationType()
            .defaultValue(Duration.ofSeconds(1L))
            .withDescription("the flush interval mills, over this time, asynchronous threads will flush data. The default value is 1s.");

    public static final ConfigOption<Integer> SINK_MAX_RETRIES = ConfigOptions.key("sink.max-retries")
            .intType()
            .defaultValue(Integer.valueOf(3))
            .withDescription("the max retry times if writing records to database failed.");

    public static final ConfigOption<Boolean> SINK_WRITE_LOCAL = ConfigOptions.key("sink.write-local")
            .booleanType()
            .defaultValue(Boolean.valueOf(false))
            .withDescription("directly write to local tables in case of Distributed table.");

    public static final ConfigOption<String> SINK_PARTITION_STRATEGY = ConfigOptions.key("sink.partition-strategy")
            .stringType()
            .defaultValue("balanced")
            .withDescription("partition strategy. available: balanced, hash, shuffle.");

    public static final ConfigOption<String> SINK_PARTITION_KEY = ConfigOptions.key("sink.partition-key")
            .stringType()
            .noDefaultValue()
            .withDescription("partition key used for hash strategy.");

    public static final ConfigOption<Boolean> SINK_IGNORE_DELETE = ConfigOptions.key("sink.ignore-delete")
            .booleanType()
            .defaultValue(Boolean.valueOf(true))
            .withDescription("whether to treat update statements as insert statements and ignore deletes. defaults to true.");


    @Override
    public DynamicTableSink createDynamicTableSink(Context context) {
        FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);
        ReadableConfig config = helper.getOptions();
        helper.validate();
        try {
            validateConfigOptions(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TableSchema physicalSchema = TableSchemaUtils.getPhysicalSchema(context.getCatalogTable().getSchema());
        return new ClickHouseDynamicTableSink(physicalSchema, getOptions(config));
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> requiredOptions = new HashSet<>();
        requiredOptions.add(URL);
        requiredOptions.add(TABLE_NAME);
        return requiredOptions;
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> optionalOptions = new HashSet<>();
        optionalOptions.add(USERNAME);
        optionalOptions.add(PASSWORD);
        optionalOptions.add(DATABASE_NAME);
        optionalOptions.add(SINK_BATCH_SIZE);
        optionalOptions.add(SINK_FLUSH_INTERVAL);
        optionalOptions.add(SINK_MAX_RETRIES);
        optionalOptions.add(SINK_WRITE_LOCAL);
        optionalOptions.add(SINK_PARTITION_STRATEGY);
        optionalOptions.add(SINK_PARTITION_KEY);
        optionalOptions.add(SINK_IGNORE_DELETE);
        return optionalOptions;
    }

    private void validateConfigOptions(ReadableConfig config) throws Exception{
        String partitionStrategy = (String)config.get(SINK_PARTITION_STRATEGY);
        if (!Arrays.asList(new String[] { "hash", "balanced", "shuffle" }).contains(partitionStrategy))
            throw new IllegalArgumentException("Unknown sink.partition-strategy `" + partitionStrategy + "`");
        if (partitionStrategy.equals("hash") && !config.getOptional(SINK_PARTITION_KEY).isPresent())
            throw new IllegalArgumentException("A partition key must be provided for hash partition strategy");
        if ((config.getOptional(USERNAME).isPresent() ^ config.getOptional(PASSWORD).isPresent()))
            throw new IllegalArgumentException("Either all or none of username and password should be provided");
    }

    private ClickHouseOptions getOptions(ReadableConfig config) {
        return (new ClickHouseOptions.Builder()).withUrl((String)config.get(URL))
                .withUsername((String)config.get(USERNAME))
                .withPassword((String)config.get(PASSWORD))
                .withDatabaseName((String)config.get(DATABASE_NAME))
                .withTableName((String)config.get(TABLE_NAME))
                .withBatchSize(((Integer)config.get(SINK_BATCH_SIZE)).intValue())
                .withFlushInterval((Duration)config.get(SINK_FLUSH_INTERVAL))
                .withMaxRetries(((Integer)config.get(SINK_MAX_RETRIES)).intValue())
                .withWriteLocal((Boolean)config.get(SINK_WRITE_LOCAL))
                .withPartitionStrategy((String)config.get(SINK_PARTITION_STRATEGY))
                .withPartitionKey((String)config.get(SINK_PARTITION_KEY))
                .withIgnoreDelete(((Boolean)config.get(SINK_IGNORE_DELETE)).booleanValue())
                .build();
    }
}
