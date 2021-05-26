import { WorkflowSummaryStatistics } from "../types";
import { Paper, Table, TableHead, TableFooter, TableBody, TableRow, TableCell, TableContainer } from '@material-ui/core';

const StatisticsSummaryTable = (props: {statistics: WorkflowSummaryStatistics}) => {
    const statistics = props.statistics;
    let totalTotal = 0;
    for (const key of Object.keys(statistics.totalPerStatus)) {
        totalTotal += statistics.totalPerStatus[key].allInstances;
    }
    return (
      <TableContainer component={Paper}>
        <Table size="small" className="table-striped table-hover">
          <TableHead>
            <TableRow>
              <TableCell>State</TableCell>
              <TableCell colSpan={2}>Created</TableCell>
              <TableCell colSpan={2}>In Progress</TableCell>
              <TableCell>Executing</TableCell>
              <TableCell>Manual</TableCell>
              <TableCell>Finished</TableCell>
              <TableCell>Total</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {statistics.stats.map((item, index) => <TableRow key={item.state}>
                <TableCell>{item.state}</TableCell>
                <TableCell>{item.stats.created.allInstances}</TableCell>
                <TableCell>{item.stats.created.queuedInstances}</TableCell>
                
                <TableCell>{item.stats.inProgress.allInstances}</TableCell>
                <TableCell>{item.stats.inProgress.queuedInstances}</TableCell>

                <TableCell>{item.stats.executing.allInstances}</TableCell>
                <TableCell>{item.stats.manual.allInstances}</TableCell>
                <TableCell>{item.stats.finished.allInstances}</TableCell>

                <TableCell>{item.total}</TableCell>
              </TableRow>)}
          </TableBody>
          <TableFooter>
            <TableRow>
              <TableCell>Total</TableCell>
              <TableCell>{statistics.totalPerStatus.created.allInstances}</TableCell>
              <TableCell>{statistics.totalPerStatus.created.queuedInstances}</TableCell>
              
              <TableCell>{statistics.totalPerStatus.inProgress.allInstances}</TableCell>
              <TableCell>{statistics.totalPerStatus.inProgress.queuedInstances}</TableCell>

              <TableCell>{statistics.totalPerStatus.executing.allInstances}</TableCell>
              <TableCell>{statistics.totalPerStatus.manual.allInstances}</TableCell>
              <TableCell>{statistics.totalPerStatus.finished.allInstances}</TableCell>
              <TableCell>{totalTotal}</TableCell>
            </TableRow>
          </TableFooter>
        </Table>
      </TableContainer>
    );
  };

export { StatisticsSummaryTable };
