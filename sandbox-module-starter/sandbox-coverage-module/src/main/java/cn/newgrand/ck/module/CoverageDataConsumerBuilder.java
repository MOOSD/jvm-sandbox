package cn.newgrand.ck.module;

import cn.newgrand.ck.executor.ConsumerBuilder;
import cn.newgrand.ck.executor.DataConsumer;
import cn.newgrand.ck.reporter.DataReporter;
import cn.newgrand.ck.reporter.LogDataReporter;
import cn.newgrand.ck.pojo.MethodCoverage;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;

import java.util.ArrayList;
import java.util.List;


public class CoverageDataConsumerBuilder implements ConsumerBuilder<MethodCoverage> {

    private final DataReporter dataReporter;

    private final ConfigInfo configInfo;

    public CoverageDataConsumerBuilder(ConfigInfo configInfo) {
        this.configInfo = configInfo;
//            this.dataReporter = new HttpDataReporter(configInfo);
        this.dataReporter = new LogDataReporter(configInfo);

    }

    @Override
    public DataConsumer<MethodCoverage> getConsumer() {
        return new CoverageDataConsumer();
    }


    class CoverageDataConsumer implements DataConsumer<MethodCoverage> {

        private final List<MethodCoverage> tempCoverageList = new ArrayList<>(50);


        @Override
        public void comsumer(MethodCoverage data) {
            // 数据消费逻辑
            tempCoverageList.add(data);
            tryReport();
        }

        /**
         * 发送数据
         */
        private void tryReport(){
            if (tempCoverageList.size() < 20) {
                return;
            }
            dataReporter.report(tempCoverageList);
            tempCoverageList.clear();

        }
    }
}

