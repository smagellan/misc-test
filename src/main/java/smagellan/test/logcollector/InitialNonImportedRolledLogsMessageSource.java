package smagellan.test.logcollector;

import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.file.FileHeaders;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class InitialNonImportedRolledLogsMessageSource extends MessageProducerSupport {
    private final TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor(InitialNonImportedRolledLogsMessageSource.class.getSimpleName() + "-");
    private final Map<LogFileInfo, Collection<File>> initialNonRolledFiles;
    private final Map<File, Collection<File>> liveFileToRolledNonImportedFiles;

    public InitialNonImportedRolledLogsMessageSource(Map<LogFileInfo, Collection<File>> initialNonRolledFiles) {
        Objects.requireNonNull(initialNonRolledFiles);
        this.initialNonRolledFiles = initialNonRolledFiles;
        this.liveFileToRolledNonImportedFiles = initialNonRolledFiles.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().liveLogFile(), Map.Entry::getValue));
    }

    @Override
    protected void doStart() {
        taskExecutor.execute(this::runExec);
    }

    public Map<File, Collection<File>> getExistingLiveLogsToNonRolledFiles() {
        return liveFileToRolledNonImportedFiles;
    }

    private void runExec() {
        for (Map.Entry<LogFileInfo, Collection<File>> fileEntry : initialNonRolledFiles.entrySet()) {
            Collection<File> rolledLogFiles = fileEntry.getValue();
            if (rolledLogFiles != null && !rolledLogFiles.isEmpty()) {
                Collection<Path> rolledPaths = rolledLogFiles
                        .stream()
                        .map(File::toPath)
                        .collect(Collectors.toList());
                RolledFileMessage msg = new RolledFileMessage(rolledPaths, Map.of(
                        FileHeaders.FILENAME, fileEntry.getKey().liveLogFile(),
                        FileHeaders.ORIGINAL_FILE, fileEntry.getKey())
                );
                sendMessage(msg);
            }
        }
    }
}
