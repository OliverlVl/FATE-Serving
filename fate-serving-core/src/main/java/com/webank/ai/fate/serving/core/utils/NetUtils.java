/*
 * Copyright 2019 The FATE Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.ai.fate.serving.core.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * IP and Port Helper for RPC
 */
public class NetUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);
    //LoggerFactory.getLogger(NetUtils.class);
    // returned port range is [30000, 39999]
    private static final int RND_PORT_START = 30000;
    private static final int RND_PORT_RANGE = 10000;
    // valid port range is (0, 65535]
    private static final int MIN_PORT = 0;
    private static final int MAX_PORT = 65535;
    // 把正则表达式编译成模式对象
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}\\:\\d{1,5}$");
    private static final Pattern LOCALHOST_PATTERN = Pattern.compile("localhost\\:\\d{1,5}$");
    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");
    private static final String SPLIT_IPV4_CHARECTER = "\\.";
    private static final String SPLIT_IPV6_CHARECTER = ":";
    private static String ANYHOST_VALUE = "0.0.0.0";
    private static String LOCALHOST_KEY = "localhost";
    private static String LOCALHOST_VALUE = "127.0.0.1";
    private static volatile InetAddress LOCAL_ADDRESS = null;

    public static void main(String[] args) {
//        System.out.println(NetUtils.getLocalHost());
//        System.out.println(NetUtils.getAvailablePort());
//        System.out.println(NetUtils.getLocalAddress());
//        System.out.println(NetUtils.getLocalIp());
//        System.out.println(NetUtils.getIpByHost("127.0.0.1"));
//        System.out.println(NetUtils.getLocalAddress0(""));
    }

    // Random的缺点在于多个线程会使用同一个原子性变量，从而导致对原子变量的竞争；
    // 而ThreadLocalRandom保证每个线程都维护一个种子变量，每个线程根据自己老的种子(每个线程不同)生成新的种子，避免了竞争问题，大大提高了并发性能。
    // 空间换时间
    public static int getRandomPort() {
        return RND_PORT_START + ThreadLocalRandom.current().nextInt(RND_PORT_RANGE);
    }

    /**
     * 判断是否LocalhostAddress，如：localhost:8081
     * @param address 地址
     * @return boolean
     */
    public static boolean isLocalhostAddress(String address){
       // 通过模式对象得到匹配器对象，这个时候需要的是被匹配的字符串
       // .maches() 调用匹配器对象的功能，返回boolean类型
       // 需要使用获取功能（查找）的时候使用Pattern 判断、分割、替换功能不使用
       // .find()查找 .group()方法返回找到的子字符串
       // 这里没有使用获取功能，可以不适用Pattern
       return LOCALHOST_PATTERN.matcher(address).matches();
    }

    /**
     * 获取端口
     * @return
     */
    public static int getAvailablePort() {
        try (ServerSocket ss = new ServerSocket()) {
            ss.bind(null);
            return ss.getLocalPort();
        } catch (IOException e) {
            return getRandomPort();
        }
    }

    public static int getAvailablePort(int port) {
        if (port <= 0) {
            return getAvailablePort();
        }
        for (int i = port; i < MAX_PORT; i++) {
            try (ServerSocket ss = new ServerSocket(i)) {
                return i;
            } catch (IOException e) {
                // continue
            }
        }
        return port;
    }

    /**
     * 判断端口号是否有效
     * @param port
     * @return
     */
    public static boolean isInvalidPort(int port) {
        return port <= MIN_PORT || port > MAX_PORT;
    }

    /**
     * 判断地址是否有效，地址格式如：192.168.1.1:8080
     * @param address
     * @return
     */
    public static boolean isValidAddress(String address) {
        return ADDRESS_PATTERN.matcher(address).matches();
    }

    public static boolean isLocalHost(String host) {
        return host != null
                && (LOCAL_IP_PATTERN.matcher(host).matches()
                || host.equalsIgnoreCase(LOCALHOST_KEY));
    }

    public static boolean isAnyHost(String host) {
        return ANYHOST_VALUE.equals(host);
    }

    public static boolean isInvalidLocalHost(String host) {
        return host == null
                || host.length() == 0
                || host.equalsIgnoreCase(LOCALHOST_KEY)
                || host.equals(ANYHOST_VALUE)
                || (LOCAL_IP_PATTERN.matcher(host).matches());
    }

    public static boolean isValidLocalHost(String host) {
        return !isInvalidLocalHost(host);
    }

    public static InetSocketAddress getLocalSocketAddress(String host, int port) {
        return isInvalidLocalHost(host) ?
                new InetSocketAddress(port) : new InetSocketAddress(host, port);
    }

    static boolean isValidV4Address(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        boolean result = (name != null
                && IP_PATTERN.matcher(name).matches()
                && !ANYHOST_VALUE.equals(name)
                && !LOCALHOST_VALUE.equals(name));
        return result;
    }

    /**
     * Check if an ipv6 address
     *
     * @return true if it is reachable
     */
    static boolean isPreferIpv6Address() {
        boolean preferIpv6 = Boolean.getBoolean("java.net.preferIPv6Addresses");
        if (!preferIpv6) {
            return false;
        }
        return false;
    }

    /**
     * normalize the ipv6 Address, convert scope name to scope id.
     * e.g.
     * convert
     * fe80:0:0:0:894:aeec:f37d:23e1%en0
     * to
     * fe80:0:0:0:894:aeec:f37d:23e1%5
     * <p>
     * The %5 after ipv6 address is called scope id.
     * see java doc of {@link Inet6Address} for more details.
     *
     * @param address the input address
     * @return the normalized address, with scope id converted to int
     */
    static InetAddress normalizeV6Address(Inet6Address address) {
        String addr = address.getHostAddress();
        int i = addr.lastIndexOf('%');
        if (i > 0) {
            try {
                return InetAddress.getByName(addr.substring(0, i) + '%' + address.getScopeId());
            } catch (UnknownHostException e) {
                // ignore
                logger.debug("Unknown IPV6 address: ", e);
            }
        }
        return address;
    }

    public static String getLocalHost() {
        InetAddress address = getLocalAddress();
        return address == null ? LOCALHOST_VALUE : address.getHostAddress();
    }


    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = getLocalAddress0("");
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    private static Optional<InetAddress> toValidAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            Inet6Address v6Address = (Inet6Address) address;
            if (isPreferIpv6Address()) {
                return Optional.ofNullable(normalizeV6Address(v6Address));
            }
        }
        if (isValidV4Address(address)) {
            return Optional.of(address);
        }
        return Optional.empty();
    }


    public static String getLocalIp() {

        try {
            InetAddress inetAddress = getLocalAddress0("eth0");
            if (inetAddress != null) {
                return inetAddress.getHostAddress();
            } else {
                inetAddress = getLocalAddress0("");
            }
            if (inetAddress != null) {
                return inetAddress.getHostAddress();
            } else {
                throw new RuntimeException("can not get local ip");
            }

        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }

    private static String getIpByEthNum(String ethNum) {
        try {
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                if (ethNum.equals(netInterface.getName())) {
                    Enumeration addresses = netInterface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        ip = (InetAddress) addresses.nextElement();
                        if (ip != null && ip instanceof Inet4Address) {
                            return ip.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException e) {
            logger.error(e.getMessage(), e);
        }
        return "";
    }


    public static String getOsName() {

        String osName = System.getProperty("os.name");
        return osName;
    }


    private static InetAddress getLocalAddress0(String name) {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            Optional<InetAddress> addressOp = toValidAddress(localAddress);
            if (addressOp.isPresent()) {
                return addressOp.get();
            } else {
                localAddress = null;
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage());
        }

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (null == interfaces) {
                return localAddress;
            }
            while (interfaces.hasMoreElements()) {
                try {
                    NetworkInterface network = interfaces.nextElement();
                    if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
                        continue;
                    }
                    if (StringUtils.isNotEmpty(name)) {
                        if (!network.getName().equals(name)) {
                            continue;
                        }
                    }
                    Enumeration<InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        try {
                            Optional<InetAddress> addressOp = toValidAddress(addresses.nextElement());
                            if (addressOp.isPresent()) {
                                try {
                                    if (addressOp.get().isReachable(100)) {
                                        return addressOp.get();
                                    }
                                } catch (IOException e) {
                                    // ignore
                                }
                            }
                        } catch (Throwable e) {
                            logger.warn(e.getMessage());
                        }
                    }
                } catch (Throwable e) {
                    logger.warn(e.getMessage());
                }
            }
        } catch (Throwable e) {
            logger.warn(e.getMessage());
        }
        return localAddress;
    }


    /**
     * @param hostName
     * @return ip address or hostName if UnknownHostException
     */
    public static String getIpByHost(String hostName) {
        try {
            return InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            return hostName;
        }
    }

    public static String toAddressString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    public static InetSocketAddress toAddress(String address) {
        int i = address.indexOf(':');
        String host;
        int port;
        if (i > -1) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
            port = 0;
        }
        return new InetSocketAddress(host, port);
    }

    public static String toUrl(String protocol, String host, int port, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://");
        sb.append(host).append(':').append(port);
        if (path.charAt(0) != '/') {
            sb.append('/');
        }
        sb.append(path);
        return sb.toString();
    }

    public static void joinMulticastGroup(MulticastSocket multicastSocket, InetAddress multicastAddress) throws IOException {
        setInterface(multicastSocket, multicastAddress instanceof Inet6Address);
        multicastSocket.setLoopbackMode(false);
        multicastSocket.joinGroup(multicastAddress);
    }

    public static void setInterface(MulticastSocket multicastSocket, boolean preferIpv6) throws IOException {
        boolean interfaceSet = false;
        Enumeration interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface i = (NetworkInterface) interfaces.nextElement();
            Enumeration addresses = i.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = (InetAddress) addresses.nextElement();
                if (preferIpv6 && address instanceof Inet6Address) {
                    try {
                        if (address.isReachable(100)) {
                            multicastSocket.setInterface(address);
                            interfaceSet = true;
                            break;
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                } else if (!preferIpv6 && address instanceof Inet4Address) {
                    try {
                        if (address.isReachable(100)) {
                            multicastSocket.setInterface(address);
                            interfaceSet = true;
                            break;
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
            if (interfaceSet) {
                break;
            }
        }
    }

//    public static boolean matchIpExpression(String pattern, String host, int port) throws UnknownHostException {
//
//        // if the pattern is subnet format, it will not be allowed to config port param in pattern.
//        if (pattern.contains("/")) {
//            CIDRUtils utils = new CIDRUtils(pattern);
//            return utils.isInRange(host);
//        }
//
//
//        return matchIpRange(pattern, host, port);
//    }

    /**
     * @param pattern
     * @param host
     * @param port
     * @return
     * @throws UnknownHostException
     */
    public static boolean matchIpRange(String pattern, String host, int port) throws UnknownHostException {
        if (pattern == null || host == null) {
            throw new IllegalArgumentException("Illegal Argument pattern or hostName. Pattern:" + pattern + ", Host:" + host);
        }
        pattern = pattern.trim();
        if ("*.*.*.*".equals(pattern) || "*".equals(pattern)) {
            return true;
        }

        InetAddress inetAddress = InetAddress.getByName(host);
        boolean isIpv4 = isValidV4Address(inetAddress) ? true : false;
        String[] hostAndPort = getPatternHostAndPort(pattern, isIpv4);
        if (hostAndPort[1] != null && !hostAndPort[1].equals(String.valueOf(port))) {
            return false;
        }
        pattern = hostAndPort[0];

        String splitCharacter = SPLIT_IPV4_CHARECTER;
        if (!isIpv4) {
            splitCharacter = SPLIT_IPV6_CHARECTER;
        }
        String[] mask = pattern.split(splitCharacter);
        //check format of pattern
        checkHostPattern(pattern, mask, isIpv4);

        host = inetAddress.getHostAddress();

        String[] ipAddress = host.split(splitCharacter);
        if (pattern.equals(host)) {
            return true;
        }
        // short name condition
        if (!ipPatternContainExpression(pattern)) {
            InetAddress patternAddress = InetAddress.getByName(pattern);
            if (patternAddress.getHostAddress().equals(host)) {
                return true;
            } else {
                return false;
            }
        }
        for (int i = 0; i < mask.length; i++) {
            if ("*".equals(mask[i]) || mask[i].equals(ipAddress[i])) {
                continue;
            } else if (mask[i].contains("-")) {
                String[] rangeNumStrs = mask[i].split("-");
                if (rangeNumStrs.length != 2) {
                    throw new IllegalArgumentException("There is wrong format of ip Address: " + mask[i]);
                }
                Integer min = getNumOfIpSegment(rangeNumStrs[0], isIpv4);
                Integer max = getNumOfIpSegment(rangeNumStrs[1], isIpv4);
                Integer ip = getNumOfIpSegment(ipAddress[i], isIpv4);
                if (ip < min || ip > max) {
                    return false;
                }
            } else if ("0".equals(ipAddress[i]) && ("0".equals(mask[i]) || "00".equals(mask[i]) || "000".equals(mask[i]) || "0000".equals(mask[i]))) {
                continue;
            } else if (!mask[i].equals(ipAddress[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean ipPatternContainExpression(String pattern) {
        return pattern.contains("*") || pattern.contains("-");
    }

    private static void checkHostPattern(String pattern, String[] mask, boolean isIpv4) {
        if (!isIpv4) {
            if (mask.length != 8 && ipPatternContainExpression(pattern)) {
                throw new IllegalArgumentException("If you config ip expression that contains '*' or '-', please fill qulified ip pattern like 234e:0:4567:0:0:0:3d:*. ");
            }
            if (mask.length != 8 && !pattern.contains("::")) {
                throw new IllegalArgumentException("The host is ipv6, but the pattern is not ipv6 pattern : " + pattern);
            }
        } else {
            if (mask.length != 4) {
                throw new IllegalArgumentException("The host is ipv4, but the pattern is not ipv4 pattern : " + pattern);
            }
        }
    }

    private static String[] getPatternHostAndPort(String pattern, boolean isIpv4) {
        String[] result = new String[2];
        if (pattern.startsWith("[") && pattern.contains("]:")) {
            int end = pattern.indexOf("]:");
            result[0] = pattern.substring(1, end);
            result[1] = pattern.substring(end + 2);
            return result;
        } else if (pattern.startsWith("[") && pattern.endsWith("]")) {
            result[0] = pattern.substring(1, pattern.length() - 1);
            result[1] = null;
            return result;
        } else if (isIpv4 && pattern.contains(":")) {
            int end = pattern.indexOf(":");
            result[0] = pattern.substring(0, end);
            result[1] = pattern.substring(end + 1);
            return result;
        } else {
            result[0] = pattern;
            return result;
        }
    }

    private static Integer getNumOfIpSegment(String ipSegment, boolean isIpv4) {
        if (isIpv4) {
            return Integer.parseInt(ipSegment);
        }
        return Integer.parseInt(ipSegment, 16);
    }

}
