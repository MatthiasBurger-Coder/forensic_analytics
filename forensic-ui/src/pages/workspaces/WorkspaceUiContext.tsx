import { createContext, type FormEvent, type ReactNode, useContext, useMemo, useState } from "react";

export interface WorkspaceDraft {
  workspaceId: string;
  title: string;
  description: string;
}

export interface WorkspaceDummyRecord extends WorkspaceDraft {
  createdAt: string;
}

export type WorkspaceView = "list" | "create" | "detail" | "edit";
export type WorkspaceDetailTab = "overview" | "repositories";

const PAGE_SIZE = 5;
let nextDummyWorkspaceNumber = 2000;

const INITIAL_WORKSPACES: WorkspaceDummyRecord[] = [
  { workspaceId: "WS-1001", title: "WildFly Investigation", description: "Runtime replay workspace for WildFly exception analysis.", createdAt: "2026-06-07" },
  { workspaceId: "WS-1002", title: "Payments Checkout Audit", description: "Static and runtime evidence review for checkout failures.", createdAt: "2026-06-07" },
  { workspaceId: "WS-1003", title: "Gateway Contract Review", description: "", createdAt: "2026-06-07" },
  { workspaceId: "WS-1004", title: "BTM Generator Smoke Test", description: "Click-dummy entry for generated artifact review.", createdAt: "2026-06-07" },
  { workspaceId: "WS-1005", title: "Joern CPG Trial", description: "Optional semantic enrichment workspace.", createdAt: "2026-06-07" },
  { workspaceId: "WS-1006", title: "Repository Source Import", description: "Repository checkout workspace administration draft.", createdAt: "2026-06-07" }
];

interface WorkspaceUiState {
  actionMessage: string | null;
  currentPage: number;
  draft: WorkspaceDraft;
  pageCount: number;
  pagedWorkspaces: WorkspaceDummyRecord[];
  sidebarWorkspaces: WorkspaceDummyRecord[];
  selectedWorkspace: WorkspaceDummyRecord | null;
  selectedWorkspaceId: string | null;
  selectedWorkspaceTab: WorkspaceDetailTab;
  titleError: string | null;
  view: WorkspaceView;
  workspaces: WorkspaceDummyRecord[];
  deleteWorkspace: (workspaceId: string) => void;
  isWorkspaceInSidebar: (workspaceId: string) => boolean;
  saveWorkspace: (event: FormEvent<HTMLFormElement>) => void;
  setDraft: (draft: WorkspaceDraft) => void;
  setPage: (page: number | ((page: number) => number)) => void;
  setSelectedWorkspaceTab: (tab: WorkspaceDetailTab) => void;
  showCreate: () => void;
  showDetail: (workspace: WorkspaceDummyRecord) => void;
  showEdit: (workspace: WorkspaceDummyRecord) => void;
  showList: () => void;
  toggleWorkspaceInSidebar: (workspaceId: string) => void;
}

const WorkspaceUiContext = createContext<WorkspaceUiState | null>(null);

export const WorkspaceUiProvider = ({ children }: { children: ReactNode }) => {
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [workspaces, setWorkspaces] = useState(INITIAL_WORKSPACES);
  const [draft, setDraft] = useState<WorkspaceDraft>(emptyDraft());
  const [selectedWorkspaceId, setSelectedWorkspaceId] = useState<string | null>(null);
  const [selectedWorkspaceTab, setSelectedWorkspaceTab] = useState<WorkspaceDetailTab>("overview");
  const [view, setView] = useState<WorkspaceView>("list");
  const [sidebarWorkspaceIds, setSidebarWorkspaceIds] = useState<string[]>([]);
  const [page, setPage] = useState(1);

  const titleError = draft.title.trim() ? null : "Title is mandatory.";
  const pageCount = Math.max(1, Math.ceil(workspaces.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const selectedWorkspace = workspaces.find((workspace) => workspace.workspaceId === selectedWorkspaceId) ?? null;
  const sidebarWorkspaces = workspaces.filter((workspace) => sidebarWorkspaceIds.includes(workspace.workspaceId));
  const pagedWorkspaces = useMemo(
    () => workspaces.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE),
    [currentPage, workspaces]
  );

  const showList = () => {
    setView("list");
    setSelectedWorkspaceId(null);
    setDraft(emptyDraft());
    setActionMessage(null);
  };

  const showCreate = () => {
    setView("create");
    setSelectedWorkspaceId(null);
    setDraft(emptyDraft());
    setActionMessage(null);
  };

  const showDetail = (workspace: WorkspaceDummyRecord) => {
    setView("detail");
    setSelectedWorkspaceId(workspace.workspaceId);
    setSelectedWorkspaceTab("overview");
    setDraft(emptyDraft());
    setActionMessage(null);
  };

  const showEdit = (workspace: WorkspaceDummyRecord) => {
    setView("edit");
    setSelectedWorkspaceId(workspace.workspaceId);
    setDraft({ workspaceId: workspace.workspaceId, title: workspace.title, description: workspace.description });
    setActionMessage(null);
  };

  const saveWorkspace = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const title = draft.title.trim();
    if (!title) return;
    const nextRecord = { workspaceId: draft.workspaceId, title, description: draft.description.trim(), createdAt: new Date().toISOString().slice(0, 10) };
    setWorkspaces((current) => view === "edit" ? current.map((workspace) => workspace.workspaceId === draft.workspaceId ? nextRecord : workspace) : [nextRecord, ...current]);
    setSelectedWorkspaceId(nextRecord.workspaceId);
    setDraft(emptyDraft());
    setPage(1);
    setView("detail");
    setActionMessage(view === "edit" ? `Workspace ${nextRecord.workspaceId} updated.` : `Workspace ${nextRecord.workspaceId} created.`);
  };

  const deleteWorkspace = (workspaceId: string) => {
    setWorkspaces((current) => current.filter((workspace) => workspace.workspaceId !== workspaceId));
    setSidebarWorkspaceIds((current) => current.filter((candidate) => candidate !== workspaceId));
    if (selectedWorkspaceId === workspaceId) {
      setSelectedWorkspaceId(null);
      setView("list");
    }
    setActionMessage(`Workspace ${workspaceId} deleted with cascade cleanup.`);
    setPage((current) => Math.min(current, pageCount));
  };

  const toggleWorkspaceInSidebar = (workspaceId: string) => {
    setSidebarWorkspaceIds((current) => current.includes(workspaceId) ? current.filter((candidate) => candidate !== workspaceId) : [...current, workspaceId]);
  };

  const value: WorkspaceUiState = {
    actionMessage,
    currentPage,
    draft,
    pageCount,
    pagedWorkspaces,
    sidebarWorkspaces,
    selectedWorkspace,
    selectedWorkspaceId,
    selectedWorkspaceTab,
    titleError,
    view,
    workspaces,
    deleteWorkspace,
    isWorkspaceInSidebar: (workspaceId) => sidebarWorkspaceIds.includes(workspaceId),
    saveWorkspace,
    setDraft,
    setPage,
    setSelectedWorkspaceTab,
    showCreate,
    showDetail,
    showEdit,
    showList,
    toggleWorkspaceInSidebar
  };

  return <WorkspaceUiContext.Provider value={value}>{children}</WorkspaceUiContext.Provider>;
};

export const useWorkspaceUi = (): WorkspaceUiState => {
  const context = useContext(WorkspaceUiContext);
  if (!context) {
    throw new Error("useWorkspaceUi must be used inside WorkspaceUiProvider");
  }
  return context;
};

const emptyDraft = (): WorkspaceDraft => ({
  workspaceId: createWorkspaceId(),
  title: "",
  description: ""
});

const createWorkspaceId = (): string => `WS-${(nextDummyWorkspaceNumber += 1).toString()}`;
