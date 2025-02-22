import { React,useState } from 'react';
import './App.css';
import Login from '../Login/Login'
import MainMenu from '../Home/MainMenu'
import { Layout} from 'antd';
import logo from './white_logo.png';
import {
  BrowserRouter as Router,
  Switch,
  Route
} from 'react-router-dom';
import { OrganizationDetails } from '../Organizations/Details';
import { CreateOrganization } from '../Organizations/Create';
import { Home } from './Home';
import { WorkspaceDetails } from '../Workspaces/Details';
import { CreateWorkspace } from '../Workspaces/Create';
import { CreateModule } from '../Modules/Create';
import { ModuleList } from '../Modules/List';
import { ModuleDetails } from '../Modules/Details';
import { OrganizationSettings } from '../Settings/Settings';
import { UserSettings } from '../UserSettings/UserSettings';
import { ProfilePicture } from './ProfilePicture';
import { useAuth } from '../../config/authConfig'; 
const { Header, Footer } = Layout;
const App = () => {
  const auth = useAuth();
  const [organizationName, setOrganizationName] = useState("...");
  
  if (!auth.isAuthenticated) {
    return (
       <Login />
    )
  }
  return (
    <Router>
      <Layout className="layout">
        <Header>
          <a>
          <img className="logo" src={logo} ></img>
          </a>
          <div className="menu">
            <MainMenu organizationName={organizationName} setOrganizationName={setOrganizationName}/>
          </div>
          <div className="user">
             <ProfilePicture />
          </div>
        </Header>
        
        <Switch>
          <Route exact path="/" component={Home} />
          <Route exact path="/organizations/create">
            <CreateOrganization setOrganizationName={setOrganizationName}></CreateOrganization>
          </Route>
          <Route exact path="/organizations/:id/workspaces">
             <OrganizationDetails setOrganizationName={setOrganizationName} organizationName={organizationName} />
          </Route>
          <Route exact path="/workspaces/create" component={CreateWorkspace} />
          <Route exact path="/workspaces/:id" component={WorkspaceDetails} />
          
          <Route exact path="/organizations/:orgid/registry/create" component={CreateModule} />
          <Route exact path="/organizations/:orgid/registry">
            <ModuleList setOrganizationName={setOrganizationName} organizationName={organizationName} />
          </Route>
          <Route exact path="/organizations/:orgid/registry/:id">
            <ModuleDetails setOrganizationName={setOrganizationName} organizationName={organizationName} />
          </Route>
          <Route exact path="/organizations/:orgid/settings" component={OrganizationSettings} />
          <Route exact path="/organizations/:orgid/settings/general">
              <OrganizationSettings selectedTab="1" />
          </Route>
          <Route exact path="/organizations/:orgid/settings/teams">
              <OrganizationSettings selectedTab="2" />
          </Route>
          <Route exact path="/organizations/:orgid/settings/vcs">
             <OrganizationSettings selectedTab="3" />
          </Route>
          <Route exact path="/organizations/:orgid/settings/vcs/new/:vcsName">
             <OrganizationSettings selectedTab="3" vcsMode="new" />
          </Route>
          <Route exact path="/settings/tokens">
             <UserSettings />
          </Route>

        </Switch>
        <Footer style={{ textAlign: 'center' }}>Terrakube ©2022</Footer>
      </Layout>
    </Router>
  )
}

export default App;
