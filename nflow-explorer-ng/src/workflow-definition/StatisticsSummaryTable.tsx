import {WorkflowSummaryStatistics} from '../types';
import {
  Paper,
  Table,
  TableHead,
  TableFooter,
  TableBody,
  TableRow,
  TableCell,
  TableContainer,
  Tooltip
} from '@material-ui/core';
import {HourglassEmpty} from '@material-ui/icons';

const renderInstanceCount = (
  allInstances: number,
  queuedInstances: number = 0,
  renderZero: boolean = false
) => {
  return (
    <div style={{display: 'flex', alignItems: 'center', flexWrap: 'wrap'}}>
      <span>{renderZero ? allInstances : allInstances || ''}</span>
      {queuedInstances > 0 && (
        <span>
          &nbsp;
          <Tooltip title={`${queuedInstances} instance waiting for executor`}>
            <HourglassEmpty fontSize="small" />
          </Tooltip>
        </span>
      )}
    </div>
  );
};

const StatisticsSummaryTable = (props: {
  statistics: WorkflowSummaryStatistics;
}) => {
  const {stats, totalPerStatus} = props.statistics;

  const totalTotal = Object.keys(totalPerStatus).reduce(
    (total, key) => total + totalPerStatus[key].allInstances,
    0
  );
  const hoplaa = 1;
  return (
    <TableContainer component={Paper}>
      <Table size="small" className="table-striped table-hover">
        <TableHead>
          <TableRow>
            <TableCell>State</TableCell>
            <TableCell>Created</TableCell>
            <TableCell>In Progress</TableCell>
            <TableCell>Executing</TableCell>
            <TableCell>Manual</TableCell>
            <TableCell>Finished</TableCell>
            <TableCell>Total</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {stats.map(item => (
            <TableRow key={item.state}>
              <TableCell>{item.state}</TableCell>
              <TableCell>
                {renderInstanceCount(
                  item.stats.created.allInstances,
                  item.stats.created.queuedInstances || 0
                )}
              </TableCell>
              <TableCell>
                {renderInstanceCount(
                  item.stats.inProgress.allInstances,
                  item.stats.inProgress.queuedInstances || 0
                )}
              </TableCell>
              <TableCell>
                {renderInstanceCount(item.stats.executing.allInstances)}
              </TableCell>
              <TableCell>
                {renderInstanceCount(item.stats.manual.allInstances)}
              </TableCell>
              <TableCell>
                {renderInstanceCount(item.stats.finished.allInstances)}
              </TableCell>
              <TableCell>{renderInstanceCount(item.total, 0, true)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
        <TableFooter>
          <TableRow>
            <TableCell>Total</TableCell>
            <TableCell>
              {renderInstanceCount(
                totalPerStatus.created.allInstances,
                totalPerStatus.created.queuedInstances || 0,
                true
              )}
            </TableCell>
            <TableCell>
              {renderInstanceCount(
                totalPerStatus.inProgress.allInstances,
                totalPerStatus.inProgress.queuedInstances || 0,
                true
              )}
            </TableCell>
            <TableCell>
              {renderInstanceCount(
                totalPerStatus.executing.allInstances,
                0,
                true
              )}
            </TableCell>
            <TableCell>
              {renderInstanceCount(totalPerStatus.manual.allInstances, 0, true)}
            </TableCell>
            <TableCell>
              {renderInstanceCount(
                totalPerStatus.finished.allInstances,
                0,
                true
              )}
            </TableCell>
            <TableCell>{renderInstanceCount(totalTotal, 0, true)}</TableCell>
          </TableRow>
        </TableFooter>
      </Table>
    </TableContainer>
  );
};

export {StatisticsSummaryTable};
