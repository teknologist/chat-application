package org.benjp.portlet.notification;

import juzu.Path;
import juzu.View;
import juzu.request.HttpContext;
import juzu.template.Template;
import org.benjp.listener.ServerBootstrap;
import org.benjp.services.SpaceBean;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

import javax.inject.Inject;
import javax.portlet.PortletPreferences;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NotificationApplication extends juzu.Controller
{

  @Inject
  PortletPreferences portletPreferences;

  @Inject
  @Path("index.gtmpl")
  Template index;

  OrganizationService organizationService_;

  SpaceService spaceService_;

  @Inject
  public NotificationApplication(OrganizationService organizationService, SpaceService spaceService)
  {
    organizationService_ = organizationService;
    spaceService_ = spaceService;
  }

  @View
  public void index() throws IOException
  {
    String chatServerURL = portletPreferences.getValue("chatServerURL", "/chatServer");
    String sessionId = getSessionId(renderContext.getHttpContext());
    String remoteUser = renderContext.getSecurityContext().getRemoteUser();

    // Set user's Full Name in the DB
    saveFullName(remoteUser);

    // Set user's Spaces in the DB
    saveSpaces(remoteUser);

    index.with().set("user", remoteUser).set("sessionId", sessionId).set("chatServerURL", chatServerURL).render();
  }

  private String getSessionId(HttpContext httpContext)
  {
    for (Cookie cookie:renderContext.getHttpContext().getCookies())
    {
      if("JSESSIONID".equals(cookie.getName()))
      {
        return cookie.getValue();
      }
    }
    return null;

  }

  protected String saveFullName(String username)
  {
    String fullname = username;
    try
    {

      fullname = ServerBootstrap.getUserService().getUserFullName(username);
      if (fullname==null)
      {
        User user = organizationService_.getUserHandler().findUserByName(username);
        fullname = user.getFirstName()+" "+user.getLastName();
        ServerBootstrap.getUserService().addUserFullName(username, fullname);
      }


    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return fullname;
  }

  protected void saveSpaces(String username)
  {
    try
    {
      ListAccess<Space> spacesListAccess = spaceService_.getAccessibleSpacesWithListAccess(username);
      List<Space> spaces = Arrays.asList(spacesListAccess.load(0, spacesListAccess.getSize()));
      List<SpaceBean> beans = new ArrayList<SpaceBean>();
      for (Space space:spaces)
      {
        SpaceBean spaceBean = new SpaceBean();
        spaceBean.setDisplayName(space.getDisplayName());
        spaceBean.setGroupId(space.getGroupId());
        spaceBean.setId(space.getId());
        spaceBean.setShortName(space.getShortName());
        beans.add(spaceBean);
      }
      ServerBootstrap.getUserService().setSpaces(username, beans);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }

  }

}