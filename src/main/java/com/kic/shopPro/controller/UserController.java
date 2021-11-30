package com.kic.shopPro.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.ibatis.session.SqlSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.protobuf.TextFormat.ParseException;
import com.kic.shopPro.dao.LoginDAO;
import com.kic.shopPro.domain.ItemVO;
import com.kic.shopPro.domain.MemberVO;
import com.kic.shopPro.domain.MypageOrderVO;
import com.kic.shopPro.domain.TopItemVO;
import com.kic.shopPro.domain.VisitorGraphVO;
import com.kic.shopPro.domain.VisitorVO;
import com.kic.shopPro.service.ItemService;
import com.kic.shopPro.service.LoginService;
import com.kic.shopPro.service.RegisterService;
import com.kic.shopPro.service.VisitorService;
import com.kic.shopPro.service.mypageService;

@Controller
public class UserController {
	@Autowired
	private LoginService loginService;
	private SqlSession sqlSession;
	@Autowired
	private ItemService itemService;
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	@Autowired
	private VisitorService visitorService;
	
	@Autowired
	private RegisterService rService;
	
	@Autowired
	private mypageService mpageService;
	
	//관리자 페이지 이동
	@RequestMapping(value="/admin/adminPage", method=RequestMethod.GET)
	public String adminPageGetMethod(Model model) {
		visitorService.addVisitor();
		List<VisitorVO> visitors = visitorService.readVisitorList(); 
		List<VisitorGraphVO> visitorGraph = visitorService.readVisitorGraphList();
		double reachedCost = ((double)visitorService.reachedTotalCost()/500000) * 100; 		
		List<TopItemVO> topItemList = visitorService.readTopItemList();
		model.addAttribute("visitors", visitors);
		System.out.println("===========================" + visitorGraph.size());
		System.out.println("===========================" +visitorGraph.toString());
		System.out.println("===========================" + visitorService.reachedTotalCost());
		System.out.println("===========================" +reachedCost);
		model.addAttribute("visitorGraph", visitorGraph);
		model.addAttribute("reachedCost", reachedCost);
		model.addAttribute("topItemList", topItemList);
		return "admin/adminPage";
	}
	//마이페이지 이동
	@RequestMapping(value="/MyPage", method=RequestMethod.GET)
	public String myPageGetMethod() {
		return "MyPage";
	}
	
	@RequestMapping(value="/signup", method=RequestMethod.GET)
	public void signupGetMethod() throws Exception{
		logger.info("get signup");

	}
	
	//회원가입 후에 메인페이지 이동
	@RequestMapping(value="/signup", method=RequestMethod.POST)
	public String signupPostMethod(MemberVO vo) throws Exception {
		loginService.signup(vo);
		return "redirect:/main";
	}
	
	//로그인 확인 절차
	@RequestMapping(value="/loginProcess", method=RequestMethod.POST)
	public String loginProcessMethod(MemberVO memVO, Model model, HttpServletRequest request) throws Exception {
		HttpSession session = request.getSession();
		List<ItemVO> iVO = itemService.readAllFoodItemsMethod();
		List<ItemVO> iVO_cloth = itemService.readAllClothItemsMethod();
		try {
			MemberVO login = loginService.loginServiceMethod(memVO);
			if(login == null) {
				session.setAttribute("login", null);
			}
			else {
				System.out.println(login.getId());
				session.setAttribute("login", login);
				session.setMaxInactiveInterval(1800);
				model.addAttribute("login", login);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		model.addAttribute("foodItemList", iVO);
		model.addAttribute("clothItemList",iVO_cloth);
		return "redirect: /shopPro/main";
	}
	
	@RequestMapping(value="/pay", method=RequestMethod.POST)
	public String payForItemMethod(@RequestParam("itemid") String itemid, @RequestParam("itemcount") int itemcount) throws Exception{
		ItemVO iVO = itemService.readFoodItemByIdMethod(itemid);
		int newStored = iVO.getStored() - itemcount;
		iVO.setStored(newStored);
		itemService.updateItemStore(iVO);
		return "redirect:/main";
	}
	
	//마이페이지
	@RequestMapping(value="/mypage", method=RequestMethod.GET)
	   public String MyPageMethod(Model model,HttpServletRequest request,HttpServletRequest response) throws Exception {
	      HttpSession session=request.getSession();
	      MemberVO mypageinfo=(MemberVO)session.getAttribute("login");
	      List<MypageOrderVO> mpvo=new ArrayList<MypageOrderVO>();
	      List<String> itemname=new ArrayList<String>();
	      List<String> itemid=new ArrayList<String>();
	      if(mypageinfo==null) {
	         String msg="로그인이 되어있지 않습니다.";
	         String url="main";
	         model.addAttribute("msg",msg);
	         model.addAttribute("url",url);
	         return "alert";
	      }
	      else {
	    	 //구매한 아이템 정보 전달
	         itemid=mpageService.getItemidFromOrder(mypageinfo.getId());	
	         itemname=mpageService.getItemnames(itemid);
	         mpvo=mpageService.getItemsFromOrderMethod(mypageinfo.getId());
	         model.addAttribute("itemnamelist",itemname);
	         model.addAttribute("mypageinfo",mypageinfo);
	         model.addAttribute("mpvo",mpvo);
	      }
	      return "Mypage/mypage";
	   }
	   
	   //마이페이지 정보 수정시 필요한 패스워드 검증
	   @RequestMapping(value="/Changefinish",method=RequestMethod.GET)
	   public String Changefinish(Model model,HttpServletRequest request) throws Exception{
	      String password=request.getParameter("updatePS");
	      HttpSession session=request.getSession();
	      MemberVO mypageinfo=(MemberVO)session.getAttribute("login");
	      System.out.println("mypageinfo : "+mypageinfo.getPass());
	      System.out.println("password : "+password);
	      if(password.equals(mypageinfo.getPass())) {
	         return "Mypage/ChangeInfo";
	      }
	      else {
	         model.addAttribute("msg","비밀번호가 일치하지 않습니다.");
	         model.addAttribute("url","UpdateMeminfo");
	         return "alert";
	      }
	   }   
	   
	   //개인 정보 수정 컨트롤러
	   @RequestMapping(value="/Updatefinish",method=RequestMethod.GET)		
	   public String Updatefinish(Model model,HttpServletRequest request) throws Exception{
	      MemberVO newVO=new MemberVO();
	      HttpSession session=request.getSession();
	      MemberVO mypageinfo=(MemberVO)session.getAttribute("login");
	      System.out.println("beforeid : "+mypageinfo.getId());
	      System.out.println("beforepassword : "+mypageinfo.getPass());
	      System.out.println("changeid:"+request.getParameter("changeid"));
	      System.out.println("changePS:"+request.getParameter("changePS"));
	      System.out.println("changeADDress:"+request.getParameter("changeAddress"));
	      //객체에 정보 담아서 전달
	      newVO.setId(request.getParameter("changeid"));	
	      newVO.setPass(request.getParameter("changePS"));
	      newVO.setAddress(request.getParameter("changeAddress"));
	      int num=mpageService.UpdateMemInfo(request.getParameter("changeid"),request.getParameter("changePS"),request.getParameter("changeAddress"),mypageinfo.getId(),mypageinfo.getPass());
	      if(num>0) {		// 업데이트 성공시 1반환
	         model.addAttribute("msg","정보가 수정되었습니다.");
	         model.addAttribute("url","mypage");
	         session.removeAttribute("login");
	         session.setAttribute("login", newVO);
	         return "alert";
	      }else {
	         model.addAttribute("msg","수정오류!");
	         model.addAttribute("url","ChangeInfo");
	         return "alert";
	      }
	   }
	   
	   //업데이트된 정보로 이동
	   @RequestMapping(value="/UpdateMeminfo",method=RequestMethod.GET)
	   public String UpdateMeminfo() throws Exception{
		   	 return "Mypage/UpdateMeminfo";
	   }
	   
	   //로그아웃 컨트롤러
	   @RequestMapping(value = "/signout", method = RequestMethod.GET)
		public String signout(HttpSession session) throws Exception {
			 logger.info("get logout");
			 loginService.signout(session);
			 return "redirect:/main";
		}
		
	   //아이디 확인
		@RequestMapping(value = "/memberIdChk", method = RequestMethod.POST)
		@ResponseBody
		public String memberIdChkPOST(String id) throws Exception{
			logger.info("memberIdChk() 진입");
			logger.info(id);
			int result = loginService.idCheck(id);
			logger.info("결과값 =" + result);
			if(result != 0) {
				return "fail";	
			}else {
				return "success";	
			}
		}
}
