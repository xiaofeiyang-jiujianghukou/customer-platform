FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./

# 配置 npm 使用国内镜像
RUN npm config set registry https://registry.npmmirror.com

RUN npm install
COPY . .
RUN npm run build

FROM crpi-27zlqugq2208c0pz.cn-hangzhou.personal.cr.aliyuncs.com/xiaofeiyang930112/nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
