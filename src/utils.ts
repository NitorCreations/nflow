import { formatDistance } from "date-fns";

const formatRelativeTime = (timestamp: string | Date | undefined, relationTo: Date = new Date()) => {
  if (!timestamp) return undefined;
  const ts = new Date(timestamp);
  const diff = formatDistance(ts, relationTo, { addSuffix: true});
  return diff;
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

export { formatRelativeTime, formatTimestamp };
