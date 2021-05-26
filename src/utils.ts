import { formatDistance } from "date-fns";

const formatAgo = (timestamp?: string | Date) => {
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

const formatTimestamp = (timestamp: Date | undefined) => {
  // TODO implement better
  if (!timestamp) {
    return undefined;
  }
  if (typeof(timestamp) === 'string') {
    timestamp = new Date(timestamp);
  }
  return timestamp.toUTCString();
}

export { formatAgo, formatTimestamp };
