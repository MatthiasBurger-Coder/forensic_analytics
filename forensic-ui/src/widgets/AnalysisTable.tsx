import { Link } from "react-router-dom";

import type { RepositoryAnalysisSummary } from "@/domain/repositoryAnalysis";
import { StatusBadge } from "@/widgets/StatusBadge";

export const AnalysisTable = ({
  analyses
}: {
  analyses: RepositoryAnalysisSummary[];
}) => (
  <div className="table-wrap">
    <table>
      <thead>
        <tr>
          <th>Analysis run</th>
          <th>Repository</th>
          <th>Branch</th>
          <th>Commit</th>
          <th>Backend status</th>
          <th>Lifecycle</th>
          <th>Created</th>
        </tr>
      </thead>
      <tbody>
        {analyses.map((analysis) => (
          <tr key={analysis.analysisRunId || analysis.repositoryUrl}>
            <td>
              {analysis.analysisRunId ? (
                <Link to={`/analysis-jobs/${analysis.analysisRunId}`}>
                  {analysis.analysisRunId}
                </Link>
              ) : (
                <span className="muted-text">Unavailable</span>
              )}
            </td>
            <td className="truncate">{analysis.repositoryUrl || "Unavailable"}</td>
            <td>{analysis.branch ?? "Absent"}</td>
            <td>{analysis.commit ?? "Absent"}</td>
            <td>{analysis.status.backendStatus ?? "Unavailable"}</td>
            <td>
              <StatusBadge status={analysis.status} />
            </td>
            <td>{analysis.createdAt ?? "Unavailable"}</td>
          </tr>
        ))}
      </tbody>
    </table>
  </div>
);
