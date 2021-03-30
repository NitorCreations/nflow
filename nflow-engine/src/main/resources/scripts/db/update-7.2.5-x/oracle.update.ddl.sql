alter sequence nflow_executor_id_seq nocache
/

alter sequence nflow_workflow_id_seq nocache
/

declare
  id_with_slack number;
begin
  select max(id) + 1000 into id_with_slack from nflow_workflow_action;
  if id_with_slack > 0 then
    begin
      execute immediate 'drop sequence nflow_workflow_action_id_seq';
      exception when others then
        null;
    end;
    execute immediate 'create sequence nflow_workflow_action_id_seq start with ' || id_with_slack || ' nocache';
  end if;
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
