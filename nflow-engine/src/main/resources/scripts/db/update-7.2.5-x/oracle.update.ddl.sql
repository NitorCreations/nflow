alter sequence nflow_executor_id_seq nocache
/

alter sequence nflow_workflow_id_seq nocache
/

drop sequence nflow_workflow_action_id_seq
/

declare
  id_with_slack number;
begin
  select nvl(max(id),0) + 1000 into id_with_slack from nflow_workflow_action;
  execute immediate 'create sequence nflow_workflow_action_id_seq start with ' || id_with_slack || ' nocache';
end;
/

create or replace trigger nflow_workflow_action_insert
  before insert on nflow_workflow_action
  for each row
declare
begin
  :new.id := nflow_workflow_action_id_seq.nextval;
end;
/
