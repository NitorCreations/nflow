function TabPanel(props: any) {
  const {children, value, index, ...other} = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <div>{children}</div>}
    </div>
  );
}

export {TabPanel};
