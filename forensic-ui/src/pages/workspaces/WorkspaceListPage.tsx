import { FormEvent } from "react";
import {
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Edit3,
  Plus,
  Save,
  Trash2,
  X
} from "lucide-react";

import {
  type WorkspaceDraft,
  type WorkspaceDetailTab,
  type WorkspaceDummyRecord,
  useWorkspaceUi
} from "@/pages/workspaces/WorkspaceUiContext";

export const WorkspaceListPage = () => {
  const workspaceUi = useWorkspaceUi();
  const pageTitle = workspaceUi.selectedWorkspace &&
    (workspaceUi.view === "detail" || workspaceUi.view === "edit")
    ? workspaceUi.selectedWorkspace.title
    : "Workspaces";

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <h1>{pageTitle}</h1>
        </div>
      </header>

      {workspaceUi.actionMessage ? (
        <div className="notice" role="status">
          <CheckCircle2 size={18} aria-hidden="true" />
          {workspaceUi.actionMessage}
        </div>
      ) : null}

      {workspaceUi.view === "create" || workspaceUi.view === "edit" ? (
        <WorkspaceForm
          draft={workspaceUi.draft}
          mode={workspaceUi.view}
          onCancel={workspaceUi.selectedWorkspace ? () => workspaceUi.showDetail(workspaceUi.selectedWorkspace as WorkspaceDummyRecord) : workspaceUi.showList}
          onChange={workspaceUi.setDraft}
          onSubmit={workspaceUi.saveWorkspace}
          titleError={workspaceUi.titleError}
        />
      ) : null}

      {workspaceUi.view === "detail" && workspaceUi.selectedWorkspace ? (
        <WorkspaceDetail
          onDelete={() => workspaceUi.deleteWorkspace(workspaceUi.selectedWorkspace!.workspaceId)}
          onEdit={() => workspaceUi.showEdit(workspaceUi.selectedWorkspace!)}
          onTabChange={workspaceUi.setSelectedWorkspaceTab}
          selectedTab={workspaceUi.selectedWorkspaceTab}
          workspace={workspaceUi.selectedWorkspace}
        />
      ) : null}

      {workspaceUi.view === "list" ? (
        <section className="panel table-wrap">
          <div className="workspace-list-toolbar">
            <div className="workspace-list-actions">
              <button className="button primary" onClick={workspaceUi.showCreate} type="button">
                <Plus size={16} aria-hidden="true" />
                Create Workspace
              </button>
            </div>
          </div>

          <table className="workspace-table">
            <thead>
              <tr>
                <th className="select-column">Show</th>
                <th>ID</th>
                <th>Description</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {workspaceUi.pagedWorkspaces.map((workspace) => (
                <tr key={workspace.workspaceId}>
                  <td className="select-column">
                    <input
                      aria-label={`Show workspace ${workspace.workspaceId} in sidebar`}
                      checked={workspaceUi.isWorkspaceInSidebar(workspace.workspaceId)}
                      onChange={() => workspaceUi.toggleWorkspaceInSidebar(workspace.workspaceId)}
                      type="checkbox"
                    />
                  </td>
                  <td>
                    <button
                      className="link-button mono-cell"
                      onClick={() => workspaceUi.showDetail(workspace)}
                      type="button"
                    >
                      {workspace.workspaceId}
                    </button>
                  </td>
                  <td>{workspace.description || <span className="muted-text">Optional</span>}</td>
                  <td>{workspace.createdAt}</td>
                  <td>
                    <div className="table-actions">
                      <button
                        aria-label={`Edit workspace ${workspace.workspaceId}`}
                        className="icon-button"
                        onClick={() => workspaceUi.showEdit(workspace)}
                        title="Edit workspace"
                        type="button"
                      >
                        <Edit3 size={16} aria-hidden="true" />
                      </button>
                      <button
                        aria-label={`Delete workspace ${workspace.workspaceId} cascade`}
                        className="icon-button danger-action"
                        onClick={() => workspaceUi.deleteWorkspace(workspace.workspaceId)}
                        title="Delete workspace cascade"
                        type="button"
                      >
                        <Trash2 size={16} aria-hidden="true" />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pagination" aria-label="Workspace paging">
            <button
              className="button secondary compact"
              disabled={workspaceUi.currentPage === 1}
              onClick={() => workspaceUi.setPage((page) => Math.max(1, page - 1))}
              type="button"
            >
              <ChevronLeft size={16} aria-hidden="true" />
              Previous
            </button>
            {Array.from({ length: workspaceUi.pageCount }, (_, index) => index + 1).map((page) => (
              <button
                aria-current={page === workspaceUi.currentPage ? "page" : undefined}
                className={page === workspaceUi.currentPage ? "button primary compact" : "button secondary compact"}
                key={page}
                onClick={() => workspaceUi.setPage(page)}
                type="button"
              >
                {page}
              </button>
            ))}
            <button
              className="button secondary compact"
              disabled={workspaceUi.currentPage === workspaceUi.pageCount}
              onClick={() => workspaceUi.setPage((page) => Math.min(workspaceUi.pageCount, page + 1))}
              type="button"
            >
              Next
              <ChevronRight size={16} aria-hidden="true" />
            </button>
          </div>
        </section>
      ) : null}
    </section>
  );
};

const WorkspaceForm = ({
  draft,
  mode,
  onCancel,
  onChange,
  onSubmit,
  titleError
}: {
  draft: WorkspaceDraft;
  mode: "create" | "edit";
  onCancel: () => void;
  onChange: (draft: WorkspaceDraft) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  titleError: string | null;
}) => (
  <section className="panel form-section workspace-edit-form">
    <form aria-labelledby="workspace-form-title" onSubmit={onSubmit}>
      <div className="panel-header">
        <div>
          <h2 id="workspace-form-title">
            {mode === "edit" ? "Edit workspace" : "Create workspace"}
          </h2>
        </div>
        <button
          aria-label="Close workspace form"
          className="icon-button"
          onClick={onCancel}
          type="button"
        >
          <X size={16} aria-hidden="true" />
        </button>
      </div>
      <div className="form-row two">
        <label>
          Workspace_ID
          <input readOnly value={draft.workspaceId} />
        </label>
        <label>
          Title
          <input
            aria-invalid={titleError ? "true" : "false"}
            required
            value={draft.title}
            onChange={(event) => onChange({ ...draft, title: event.target.value })}
            placeholder="Workspace title"
          />
        </label>
      </div>
      <label>
        Description
        <textarea
          rows={4}
          value={draft.description}
          onChange={(event) => onChange({ ...draft, description: event.target.value })}
          placeholder="Optional workspace description"
        />
      </label>
      <p className="muted-text">
        Delete uses cascade semantics in this click dummy. No backend data is
        changed by this local UI interaction.
      </p>
      <div className="button-row">
        <button className="button primary" disabled={Boolean(titleError)} type="submit">
          <Save size={16} aria-hidden="true" />
          {mode === "edit" ? "Update" : "Create"}
        </button>
        <button className="button secondary" onClick={onCancel} type="button">
          <X size={16} aria-hidden="true" />
          Cancel
        </button>
      </div>
    </form>
  </section>
);

const WorkspaceDetail = ({
  onDelete,
  onEdit,
  onTabChange,
  selectedTab,
  workspace
}: {
  onDelete: () => void;
  onEdit: () => void;
  onTabChange: (tab: WorkspaceDetailTab) => void;
  selectedTab: WorkspaceDetailTab;
  workspace: WorkspaceDummyRecord;
}) => (
  <section className="workspace-detail-view">
    <div className="workspace-detail-tabs" role="tablist" aria-label="Workspace sections">
      <button
        aria-selected={selectedTab === "overview"}
        className={selectedTab === "overview" ? "workspace-tab active" : "workspace-tab"}
        onClick={() => onTabChange("overview")}
        role="tab"
        type="button"
      >
        Overview
      </button>
      <button
        aria-selected={selectedTab === "repositories"}
        className={selectedTab === "repositories" ? "workspace-tab active" : "workspace-tab"}
        onClick={() => onTabChange("repositories")}
        role="tab"
        type="button"
      >
        Repositories
      </button>
    </div>

    {selectedTab === "overview" ? (
      <WorkspaceOverview onDelete={onDelete} onEdit={onEdit} workspace={workspace} />
    ) : (
      <WorkspaceRepositories workspace={workspace} />
    )}
  </section>
);

const WorkspaceOverview = ({
  onDelete,
  onEdit,
  workspace
}: {
  onDelete: () => void;
  onEdit: () => void;
  workspace: WorkspaceDummyRecord;
}) => (
  <section className="panel workspace-detail" role="tabpanel">
      <div className="panel-header">
        <div />
        <div className="table-actions">
          <button className="button secondary" onClick={onEdit} type="button">
            <Edit3 size={16} aria-hidden="true" />
            Edit
          </button>
          <button className="button secondary danger-action" onClick={onDelete} type="button">
            <Trash2 size={16} aria-hidden="true" />
            Delete
          </button>
        </div>
      </div>
      <dl className="detail-grid">
        <div>
          <dt>Workspace_ID</dt>
          <dd>{workspace.workspaceId}</dd>
        </div>
        <div>
          <dt>Created</dt>
          <dd>{workspace.createdAt}</dd>
        </div>
        <div>
          <dt>Description</dt>
          <dd>{workspace.description || "Optional"}</dd>
        </div>
      </dl>
    </section>
);

const WorkspaceRepositories = ({ workspace }: { workspace: WorkspaceDummyRecord }) => (
  <section className="panel workspace-detail" role="tabpanel">
    <div className="panel-header">
      <div>
        <h3>Repositories</h3>
        <p className="muted-text">
          Click dummy for repositories assigned to {workspace.title}.
        </p>
      </div>
      <button className="button primary" type="button">
        <Plus size={16} aria-hidden="true" />
        Add repository
      </button>
    </div>
    <div className="repository-ribbon-list">
      <article className="repository-card">
        <strong>source-analysis-service</strong>
        <span>Checkout ready</span>
      </article>
      <article className="repository-card">
        <strong>runtime-evidence-importer</strong>
        <span>Pending configuration</span>
      </article>
    </div>
  </section>
);
