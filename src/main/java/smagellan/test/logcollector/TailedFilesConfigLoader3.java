package smagellan.test.logcollector;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowDefinition;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.dsl.Files;
import org.springframework.integration.file.dsl.TailAdapterSpec;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Configuration
@EnableIntegration
public class TailedFilesConfigLoader3 {
    public static final String TAIL_FROM_FILE_START_OPTIONS = "-F -n 2147483647";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TailedFilesConfigLoader3.class);


    private final InitialNonImportedRolledLogsMessageSource initialRolledNonImportedLogs;
    private final Collection<File> filesList;
    private final List<String> tailFlowsIds;
    private final IntegrationFlowContext ctx;

    @Autowired
    public TailedFilesConfigLoader3(LogCollectorConfig collectorConfig, InitialNonImportedRolledLogsMessageSource initialRolledNonImportedLogs, IntegrationFlowContext ctx) {
        this.ctx = ctx;
        this.filesList = collectorConfig.liveFileToRolledFile().keySet();
        this.initialRolledNonImportedLogs = initialRolledNonImportedLogs;
        this.tailFlowsIds = Collections.unmodifiableList(buildFileTailerIds());
    }

    @Bean
    @Qualifier(Constants.CONTROL_BUS)
    public IntegrationFlow controlBus() {
        return IntegrationFlowDefinition::controlBus;
    }

    private List<String> buildFileTailerIds() {
        return IntStream.range(0, filesList.size())
                .boxed()
                .map(idx -> "fileTailer" + idx)
                .collect(Collectors.toList());
    }

    @Bean
    @Qualifier(Constants.FILE_TAILER_IDS)
    public List<String> fileTailerIds() {
        return tailFlowsIds;
    }

    @PostConstruct
    private void registerTailedLogsFlows() {
        logger.info("registering log tailers");
        Map<File, Collection<File>> nonRolledFiles = this.initialRolledNonImportedLogs.getExistingNonRolledFiles();
        Iterator<String> tailerIds = tailFlowsIds.iterator();
        for (File file : filesList) {
            String registrationId = tailerIds.next();
            logger.info("{} -> {}", registrationId, file);
            boolean trackSinceBeginning = nonRolledFiles.get(file) != null && !nonRolledFiles.get(file).isEmpty();
            ctx.registration(convertingTailedFileFlow(file, trackSinceBeginning, Constants.TAILED_AND_ROLLED_FILES_CHANNEL))
                    .id(registrationId)
                    .autoStartup(false)
                    .register();
        }
    }

    private IntegrationFlow convertingTailedFileFlow(File file, boolean trackSinceBeginning, String outputChannelName) {
        TailAdapterSpec spec = Files.tailAdapter(file);
        if (trackSinceBeginning) {
            spec = spec.nativeOptions(TAIL_FROM_FILE_START_OPTIONS);
        }
        return IntegrationFlows
                .from(spec)
                .transform(new LogLineTransformer())
                .channel(outputChannelName)
                .get();
    }
}
