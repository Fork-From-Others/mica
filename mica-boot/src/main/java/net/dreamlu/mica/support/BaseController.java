/*
 * Copyright (c) 2019-2029, Dreamlu 卢春梦 (596392912@qq.com & www.dreamlu.net).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dreamlu.mica.support;

import net.dreamlu.mica.core.result.IResultCode;
import net.dreamlu.mica.core.result.R;
import net.dreamlu.mica.core.result.SystemCode;
import net.dreamlu.mica.core.utils.Charsets;
import net.dreamlu.mica.core.utils.StringPool;
import net.dreamlu.mica.core.utils.WebUtil;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.util.UriUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

/**
 * 基础 controller
 *
 * @author L.cm
 */
public abstract class BaseController {

	/**
	 * redirect跳转
	 *
	 * @param url 目标url
	 * @return 跳转地址
	 */
	protected String redirect(String url) {
		return "redirect:".concat(url);
	}

	/**
	 * 返回成功
	 * @param <T> 泛型标记
	 * @return Result
	 */
	protected <T> R<T> success() {
		return R.success();
	}

	/**
	 * 成功-携带数据
	 *
	 * @param data 数据
	 * @param <T> 泛型标记
	 * @return Result
	 */
	protected <T> R<T> success(@Nullable T data) {
		return R.success(data);
	}

	/**
	 * 根据状态返回成功或者失败
	 * @param status 状态
	 * @param msg 异常msg
	 * @param <T> 泛型标记
	 * @return Result
	 */
	protected <T> R<T> status(boolean status, String msg) {
		return R.status(status, msg);
	}

	/**
	 * 根据状态返回成功或者失败
	 * @param status 状态
	 * @param sCode 异常code码
	 * @param <T> 泛型标记
	 * @return Result
	 */
	protected <T> R<T> status(boolean status, IResultCode sCode) {
		return R.status(status, sCode);
	}

	/**
	 * 返回失败信息，用于 web
	 *
	 * @param msg 失败信息
	 * @param <T> 泛型标记
	 * @return {Result}
	 */
	protected <T> R<T> fail(String msg) {
		return R.fail(SystemCode.FAILURE, msg);
	}

	/**
	 * 返回失败信息
	 *
	 * @param rCode 异常枚举
	 * @param <T> 泛型标记
	 * @return {Result}
	 */
	protected <T> R<T> fail(IResultCode rCode) {
		return R.fail(rCode);
	}

	/**
	 * 返回失败信息
	 *
	 * @param rCode 异常枚举
	 * @param msg 失败信息
	 * @param <T> 泛型标记
	 * @return {Result}
	 */
	protected <T> R<T> fail(IResultCode rCode, String msg) {
		return R.fail(rCode, msg);
	}

	/**
	 * 下载文件
	 *
	 * @param file 文件
	 * @return {ResponseEntity}
	 * @throws IOException io异常
	 */
	protected ResponseEntity<ResourceRegion> download(File file) throws IOException {
		String fileName = file.getName();
		return download(file, fileName);
	}

	/**
	 * 下载
	 *
	 * @param file     文件
	 * @param fileName 生成的文件名
	 * @return {ResponseEntity}
	 * @throws IOException io异常
	 */
	protected ResponseEntity<ResourceRegion> download(File file, String fileName) throws IOException {
		Resource resource = new FileSystemResource(file);
		return download(resource, fileName);
	}

	/**
	 * 下载
	 *
	 * @param resource 资源
	 * @param fileName 生成的文件名
	 * @return {ResponseEntity}
	 * @throws IOException io异常
	 */
	protected ResponseEntity<ResourceRegion> download(Resource resource, String fileName) throws IOException {
		HttpServletRequest request = WebUtil.getRequest();
		String header = request.getHeader("User-Agent");
		// 避免空指针
		header = header == null ? StringPool.EMPTY : header.toUpperCase();
		HttpStatus status;
		if (header.contains("MSIE") || header.contains("TRIDENT") || header.contains("EDGE")) {
			status = HttpStatus.OK;
		} else {
			status = HttpStatus.CREATED;
		}
		// 断点续传
		long position = 0;
		long count = resource.contentLength();
		String range = request.getHeader("Range");
		if (null != range) {
			status = HttpStatus.PARTIAL_CONTENT;
			String[] rangeRange = range.replace("bytes=", StringPool.EMPTY).split(StringPool.DASH);
			position = Long.parseLong(rangeRange[0]);
			if (rangeRange.length > 1) {
				long end = Long.parseLong(rangeRange[1]);
				count = end - position + 1;
			}
		}
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		String encodeFileName = UriUtils.encode(fileName, Charsets.UTF_8);
		// 兼容各种浏览器下载：
		// https://blog.robotshell.org/2012/deal-with-http-header-encoding-for-file-download/
		String disposition = "attachment;" +
			"filename=\"" + encodeFileName + "\";" +
			"filename*=utf-8''" + encodeFileName;
		headers.set("Content-Disposition", disposition);
		return new ResponseEntity<>(new ResourceRegion(resource, position, count), headers, status);
	}
}
