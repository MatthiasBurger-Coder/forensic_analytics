package de.burger.forensics.analytics.application.ingestion;

import de.burger.forensics.analytics.application.ingestion.command.AbortAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.CompleteAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.StartAnalysisSessionCommand;
import de.burger.forensics.analytics.application.ingestion.command.UploadAnalysisDataCommand;
import de.burger.forensics.analytics.application.ingestion.result.AbortAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.CompleteAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.StartAnalysisSessionResult;
import de.burger.forensics.analytics.application.ingestion.result.UploadAnalysisDataResult;

public interface ForensicIngestionUseCase {
    StartAnalysisSessionResult start(StartAnalysisSessionCommand command);

    UploadAnalysisDataResult upload(UploadAnalysisDataCommand command);

    CompleteAnalysisSessionResult complete(CompleteAnalysisSessionCommand command);

    AbortAnalysisSessionResult abort(AbortAnalysisSessionCommand command);
}
