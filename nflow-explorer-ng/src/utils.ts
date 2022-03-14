import {formatDistance, format} from 'date-fns';

const formatRelativeTime = (
  timestamp: string | Date | undefined,
  relationTo: Date = new Date()
) => {
  if (!timestamp) return undefined;
  const ts = new Date(timestamp);
  const diff = formatDistance(ts, relationTo, {addSuffix: true});
  return diff;
};

const formatTimestamp = (timestamp: string | Date | undefined) => {
  // TODO implement better
  if (!timestamp) {
    return undefined;
  }
  if (typeof timestamp === 'string') {
    timestamp = new Date(timestamp);
  }
  return format(timestamp, 'yyyy-MM-dd kk:mm:ss');
};

export {formatRelativeTime, formatTimestamp};
