export interface EvidenceClassItem {
  label: string;
  value: string;
}

export const EvidenceClassGrid = ({ items }: { items: EvidenceClassItem[] }) => (
  <dl className="evidence-class-grid">
    {items.map((item) => (
      <div key={item.label}>
        <dt>{item.label}</dt>
        <dd>{item.value}</dd>
      </div>
    ))}
  </dl>
);
