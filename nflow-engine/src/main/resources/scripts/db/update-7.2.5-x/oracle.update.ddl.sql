alter sequence nflow_executor_id_seq nocache
/

alter sequence nflow_workflow_id_seq nocache
/

alter sequence nflow_workflow_action_id_seq start with nflow_workflow_id_seq.nextval nocache
/

create or replace trigger nflow_workflow_action_insert
  before insert on nflow_workflow_action
  for each row
declare
begin
  :new.id := nflow_workflow_action_id_seq.nextval;
end;
/
