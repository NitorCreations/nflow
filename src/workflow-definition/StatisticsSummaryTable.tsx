import { WorkflowSummaryStatistics } from "../types";

const StatisticsSummaryTable = (props: {statistics: WorkflowSummaryStatistics}) => {
    const statistics = props.statistics;
    let totalTotal = 0;
    for (const key of Object.keys(statistics.totalPerStatus)) {
        totalTotal += statistics.totalPerStatus[key].allInstances;
    }
    return (
      <table>
        <thead>
          <tr>
            <th>State</th>
            <th colSpan={2}>Created</th>
            <th colSpan={2}>In Progress</th>
            <th>Executing</th>
            <th>Manual</th>
            <th>Finished</th>
            <th>Total</th>
          </tr>
        </thead>
        <tbody>
          {statistics.stats.map((item, index) => <tr key={item.state}>
              <td>{item.state}</td>
              <td>{item.stats.created.allInstances}</td>
              <td>{item.stats.created.queuedInstances}</td>
              
              <td>{item.stats.inProgress.allInstances}</td>
              <td>{item.stats.inProgress.queuedInstances}</td>

              <td>{item.stats.executing.allInstances}</td>
              <td>{item.stats.manual.allInstances}</td>
              <td>{item.stats.finished.allInstances}</td>

              <td>{item.total}</td>

            </tr>)}
        </tbody>
        <tfoot>
          <tr>
            <th>Total</th>
            <td>{statistics.totalPerStatus.created.allInstances}</td>
            <td>{statistics.totalPerStatus.created.queuedInstances}</td>
            
            <td>{statistics.totalPerStatus.inProgress.allInstances}</td>
            <td>{statistics.totalPerStatus.inProgress.queuedInstances}</td>

            <td>{statistics.totalPerStatus.executing.allInstances}</td>
            <td>{statistics.totalPerStatus.manual.allInstances}</td>
            <td>{statistics.totalPerStatus.finished.allInstances}</td>
            <td>{totalTotal}</td>
          </tr>
        </tfoot>
      </table>
    );
  };

export { StatisticsSummaryTable };
