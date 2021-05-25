import { formatDistance } from "date-fns";

const formatAgo = (timestamp?: string) => {
  if (!timestamp) return undefined;
  const ts = new Date(timestamp);
  const now = new Date();
  const diff = formatDistance(ts, now);
  if (ts < now) {
    return diff + " ago";
  }
  if (ts > now) {
    return "in " + diff;
  }
  return "now";
};

export { formatAgo };
