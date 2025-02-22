import { React, useEffect, useState } from "react";
import axiosInstance, { axiosClient } from "../../config/axiosConfig";
import {
  ORGANIZATION_ARCHIVE,
  WORKSPACE_ARCHIVE,
  ORGANIZATION_NAME,
} from "../../config/actionTypes";
import {
  Button,
  Layout,
  Breadcrumb,
  Tabs,
  List,
  Avatar,
  Tag,
  Form,
  Input,
  Select,
  message,
  Spin,
  Switch,
  Typography,
} from "antd";
import { compareVersions } from "./Workspaces";
import { CreateJob } from "../Jobs/Create";
import { DetailsJob } from "../Jobs/Details";
import { Variables } from "../Workspaces/Variables";
import { States } from "../Workspaces/States";
import { Schedules } from "../Workspaces/Schedules";
import { useParams, Link } from "react-router-dom";
import {
  CheckCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  ExclamationCircleOutlined,
  StopOutlined,
  InfoCircleOutlined,
} from "@ant-design/icons";
import "./Workspaces.css";
const { TabPane } = Tabs;
const { Option } = Select;
const { Paragraph } = Typography;
const include = {
  VARIABLE: "variable",
  JOB: "job",
  HISTORY: "history",
  SCHEDULE: "schedule",
};
const { DateTime } = require("luxon");

const { Content } = Layout;

export const WorkspaceDetails = (props) => {
  const { id } = useParams();
  const organizationId = localStorage.getItem(ORGANIZATION_ARCHIVE);
  localStorage.setItem(WORKSPACE_ARCHIVE, id);
  const [workspace, setWorkspace] = useState({});
  const [variables, setVariables] = useState([]);
  const [history, setHistory] = useState([]);
  const [schedule, setSchedule] = useState([]);
  const [envVariables, setEnvVariables] = useState([]);
  const [jobs, setJobs] = useState([]);
  const [stateDetailsVisible, setStateDetailsVisible] = useState(false);
  const [jobId, setJobId] = useState(0);
  const [loading, setLoading] = useState(false);
  const [jobVisible, setjobVisible] = useState(false);
  const [organizationName, setOrganizationName] = useState([]);
  const [workspaceName, setWorkspaceName] = useState("...");
  const [activeKey, setActiveKey] = useState("2");
  const [terraformVersions, setTerraformVersions] = useState([]);
  const [waiting, setWaiting] = useState(false);
  const [templates, setTemplates] = useState([]);
  const [lastRun, setLastRun] = useState("");
  const terraformVersionsApi =
    `${new URL(window._env_.REACT_APP_TERRAKUBE_API_URL).origin}/terraform/index.json`;
  const handleClick = (id) => {
    changeJob(id);
  };

  const handleStatesClick = (key) => {
    switchKey(key);
  };
  const callback = (key) => {
    switchKey(key);
  };

  const switchKey = (key) => {
    setActiveKey(key);
    if (key == "2") {
      setjobVisible(false);
    }
    if (key == "3") {
      setStateDetailsVisible(false);
    }
  };
  useEffect(() => {
    setLoading(true);
    loadWorkspace();
    axiosClient.get(terraformVersionsApi).then((resp) => {
      console.log(resp);
      const tfVersions = [];
      for (const version in resp.data.versions) {
        if (!version.includes("-")) tfVersions.push(version);
      }
      setTerraformVersions(tfVersions.sort(compareVersions).reverse());
      console.log(tfVersions);
    });
    setLoading(false);
    const interval = setInterval(() => {
      loadWorkspace();
    }, 15000);
    return () => clearInterval(interval);
  }, [id]);

  const changeJob = (id) => {
    console.log(id);
    setJobId(id);
    setjobVisible(true);
    setActiveKey("2");
  };

  const loadWorkspace = () => {
    axiosInstance
      .get(`organization/${organizationId}/template`)
      .then((template) => {
        setTemplates(template.data.data);
        axiosInstance
          .get(
            `organization/${organizationId}/workspace/${id}?include=job,variable,history,schedule`
          )
          .then((response) => {
            console.log(response);
            setWorkspace(response.data);
            if (response.data.included) {
              setupWorkspaceIncludes(
                response.data.included,
                setVariables,
                setJobs,
                setEnvVariables,
                setHistory,
                setSchedule,
                template.data.data,
                setLastRun
              );
            }
            setOrganizationName(localStorage.getItem(ORGANIZATION_NAME));
            setWorkspaceName(response.data.data.attributes.name);
          });
      });
  };

  const onFinish = (values) => {
    setWaiting(true);
    const body = {
      data: {
        type: "workspace",
        id: id,
        attributes: {
          name: values.name,
          description: values.description,
          folder: values.folder,
          locked: values.locked,
          terraformVersion: values.terraformVersion,
        },
      },
    };
    console.log(body);

    axiosInstance
      .patch(`organization/${organizationId}/workspace/${id}`, body, {
        headers: {
          "Content-Type": "application/vnd.api+json",
        },
      })
      .then((response) => {
        console.log(response);
        if (response.status == "204") {
          message.success("Workspace updated successfully");
        } else {
          message.error("Workspace update failed");
        }
        setWaiting(false);
      });
  };

  return (
    <Content style={{ padding: "0 50px" }}>
      <Breadcrumb style={{ margin: "16px 0" }}>
        <Breadcrumb.Item>{organizationName}</Breadcrumb.Item>
        <Breadcrumb.Item>
          <Link to={`/organizations/${organizationId}/workspaces`}>
            Workspaces
          </Link>
        </Breadcrumb.Item>
        <Breadcrumb.Item>{workspaceName}</Breadcrumb.Item>
      </Breadcrumb>
      <div className="site-layout-content">
        <div className="workspaceDisplay">
          {loading || !workspace.data || !variables || !jobs ? (
            <p>Data loading...</p>
          ) : (
            <div className="orgWrapper">
              <div className="variableActions">
                <h2>{workspace.data.attributes.name}</h2>
                <table className="moduleDetails">
                  <tr>
                    <td>Resources</td>
                    <td>Terraform version</td>
                    <td>Updated</td>
                  </tr>
                  <tr className="black">
                    <td>0</td>
                    <td>{workspace.data.attributes.terraformVersion}</td>
                    <td>
                      {DateTime.fromISO(lastRun).toRelative() ??
                        "never executed"}
                    </td>
                  </tr>
                </table>
              </div>
              <div className="App-text">
                {workspace.data.attributes.description}
              </div>
              <Tabs
                activeKey={activeKey}
                defaultActiveKey="1"
                onTabClick={handleStatesClick}
                tabBarExtraContent={<CreateJob changeJob={changeJob} />}
                onChange={callback}
              >
                <TabPane tab="Runs" key="2">
                  {jobVisible ? (
                    <DetailsJob jobId={jobId} />
                  ) : (
                    <div>
                      <h3>Run List</h3>
                      <List
                        itemLayout="horizontal"
                        dataSource={jobs
                          .sort((a, b) => a.id - b.id)
                          .reverse()}
                        renderItem={(item) => (
                          <List.Item
                            extra={
                              <div className="textLeft">
                                <Tag
                                  icon={
                                    item.status == "completed" ? (
                                      <CheckCircleOutlined />
                                    ) : item.status == "running" ? (
                                      <SyncOutlined spin />
                                    ) : item.status === "waitingApproval" ? (
                                      <ExclamationCircleOutlined />
                                    ) : item.status === "cancelled" ? (
                                      <StopOutlined />
                                    ) : item.status === "failed" ? (
                                      <StopOutlined />
                                    ) : (
                                      <ClockCircleOutlined />
                                    )
                                  }
                                  color={item.statusColor}
                                >
                                  {item.status}
                                </Tag>{" "}
                                <br />
                                <span className="metadata">
                                  {item.latestChange}
                                </span>
                              </div>
                            }
                          >
                            <List.Item.Meta
                              style={{ margin: "0px", padding: "0px" }}
                              avatar={
                                <Avatar
                                  shape="square"
                                  src="https://avatarfiles.alphacoders.com/128/thumb-128984.png"
                                />
                              }
                              title={
                                <a onClick={() => handleClick(item.id)}>
                                  {item.title}
                                </a>
                              }
                              description={
                                <span>
                                  {" "}
                                  #job-{item.id} | <b>{item.createdBy}</b>{" "}
                                  triggered via UI
                                </span>
                              }
                            />
                          </List.Item>
                        )}
                      />
                    </div>
                  )}
                </TabPane>
                <TabPane tab="States" key="3">
                  <States
                    history={history}
                    setStateDetailsVisible={setStateDetailsVisible}
                    stateDetailsVisible={stateDetailsVisible}
                  />
                </TabPane>
                <TabPane tab="Variables" key="4">
                  <Variables vars={variables} env={envVariables} />
                </TabPane>
                <TabPane tab="Schedules" key="5">
                  {templates ? (
                    <Schedules schedules={schedule} />
                  ) : (
                    <p>Loading...</p>
                  )}
                </TabPane>
                <TabPane tab="Settings" key="6">
                  <div className="generalSettings">
                    <h1>General Settings</h1>
                    <Spin spinning={waiting}>
                      <Form
                        onFinish={onFinish}
                        initialValues={{
                          name: workspace.data.attributes.name,
                          description: workspace.data.attributes.description,
                          folder: workspace.data.attributes.folder,
                          locked: workspace.data.attributes.locked,
                        }}
                        layout="vertical"
                        name="form-settings"
                      >
                        <Form.Item name="id" label="ID">
                          <Paragraph copyable={{ tooltips: false }}>
                            <span className="App-text"> {id}</span>
                          </Paragraph>
                        </Form.Item>
                        <Form.Item name="name" label="Name">
                          <Input />
                        </Form.Item>

                        <Form.Item
                          valuePropName="value"
                          name="description"
                          label="Description"
                        >
                          <Input.TextArea placeholder="Workspace description" />
                        </Form.Item>
                        <Form.Item
                          name="terraformVersion"
                          label="Terraform Version"
                          extra="The version of Terraform to use for this workspace.
                          Upon creating this workspace, the latest version was
                          selected and will be used until it is changed
                          manually. It will not upgrade automatically."
                        >
                          <Select
                            defaultValue={
                              workspace.data.attributes.terraformVersion
                            }
                            style={{ width: 250 }}
                          >
                            {terraformVersions.map(function (name, index) {
                              return <Option key={name}>{name}</Option>;
                            })}
                          </Select>
                        </Form.Item>
                        <Form.Item
                          name="folder"
                          label="Terraform Working Directory"
                          extra="The directory that Terraform will execute within. This defaults to the root of your repository and is typically set to a subdirectory matching the environment when multiple environments exist within the same repository."
                        >
                          <Input />
                        </Form.Item>
                        <Form.Item
                          name="locked"
                          valuePropName="checked"
                          label="Lock Workspace"
                          tooltip={{
                            title: "Lock Workspace",
                            icon: <InfoCircleOutlined />,
                          }}
                        >
                          <Switch />
                        </Form.Item>

                        <Form.Item>
                          <Button type="primary" htmlType="submit">
                            Save settings
                          </Button>
                        </Form.Item>
                      </Form>
                    </Spin>
                  </div>
                </TabPane>
              </Tabs>
            </div>
          )}
        </div>
      </div>
    </Content>
  );
};

function setupWorkspaceIncludes(
  includes,
  setVariables,
  setJobs,
  setEnvVariables,
  setHistory,
  setSchedule,
  templates,
  setLastRun
) {
  let variables = [];
  let jobs = [];
  let envVariables = [];
  let history = [];
  let schedule = [];

  includes.forEach((element) => {
    switch (element.type) {
      case include.JOB:
        let finalColor = "";
        switch (element.attributes.status) {
          case "completed":
            finalColor = "#2eb039";
            break;
          case "rejected":
            finalColor = "#FB0136";
            break;
          case "failed":
              finalColor = "#FB0136";
              break;
          case "running":
            finalColor = "#108ee9";
            break;
          case "waitingApproval":
            finalColor = "#fa8f37";
            break;
          default:
            finalColor = "";
            break;
        }
        jobs.push({
          id: element.id,
          title: "Queue manually using Terraform",
          statusColor: finalColor,
          latestChange: DateTime.fromISO(
            element.attributes.createdDate
          ).toRelative(),
          ...element.attributes,
        });
        setLastRun(element.attributes.updatedDate);
        break;
      case include.HISTORY:
        history.push({
          id: element.id,
          title: "Queue manually using Terraform",
          relativeDate: DateTime.fromISO(
            element.attributes.createdDate
          ).toRelative(),
          ...element.attributes,
        });
        break;

      case include.SCHEDULE:
        schedule.push({
          id: element.id,
          name: templates?.find(
            (template) => template.id === element.attributes.templateReference
          )?.attributes?.name,
          ...element.attributes,
        });
        break;
      case include.VARIABLE:
        if (element.attributes.category == "ENV") {
          envVariables.push({
            id: element.id,
            type: element.type,
            ...element.attributes,
          });
        } else {
          variables.push({
            id: element.id,
            type: element.type,
            ...element.attributes,
          });
        }
        break;
    }
  });

  setVariables(variables);
  setEnvVariables(envVariables);
  setJobs(jobs);
  setHistory(history);
  setSchedule(schedule);
}
